using MedicationManagement.Models;
using MedicationManagement.Models.DTOs;
using MedicationManagement.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;

namespace MedicationManagement.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class AuthController : ControllerBase
    {
        private readonly UserManager<ApplicationUser> _userManager;
        private readonly SignInManager<ApplicationUser> _signInManager;
        private readonly RoleManager<IdentityRole> _roleManager;
        private readonly IConfiguration _configuration;
        private readonly ILogger<AuthController> _logger;
        private readonly IServiceAuditLog _auditLogService;

        public AuthController(UserManager<ApplicationUser> userManager,
                              SignInManager<ApplicationUser> signInManager,
                              RoleManager<IdentityRole> roleManager,
                              IConfiguration configuration,
                              ILogger<AuthController> logger,
                              IServiceAuditLog auditLogService)
        {
            _userManager = userManager;
            _signInManager = signInManager;
            _roleManager = roleManager;
            _configuration = configuration;
            _logger = logger;
            _auditLogService = auditLogService;
        }

        [HttpPost("register")]
        public async Task<IActionResult> Register([FromBody] Register model)
        {
            if (!ModelState.IsValid)
                return BadRequest(ModelState);

            try
            {
                var userExists = await _userManager.FindByEmailAsync(model.Email);
                if (userExists != null)
                    return Conflict("User already exists!");

                // Перевіряємо ДО CreateAsync, щоб уникнути race condition
                var isFirstUser = !await _userManager.Users.AnyAsync();

                var user = new ApplicationUser
                {
                    UserName = model.Email,
                    Email = model.Email,
                    EmailConfirmed = true,
                    SecurityStamp = Guid.NewGuid().ToString(),
                    OrganizationId = Guid.NewGuid().ToString()
                };

                var result = await _userManager.CreateAsync(user, model.Password);
                if (!result.Succeeded)
                    return BadRequest(result.Errors);

                var role = isFirstUser ? "Administrator" : "User";
                await _userManager.AddToRoleAsync(user, role);

                await _auditLogService.LogAction("Register", model.Email, $"Registered new user with role {role}.", false);

                return Ok("User registered successfully!");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error during user registration");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpPost("login")]
        public async Task<IActionResult> Login([FromBody] Login model)
        {
            try
            {
                var user = await _userManager.FindByEmailAsync(model.Email);
                if (user == null)
                    return Unauthorized("Invalid login attempt");

                var result = await _signInManager.CheckPasswordSignInAsync(user, model.Password, false);
                if (!result.Succeeded)
                    return Unauthorized("Invalid login attempt");

                await _auditLogService.LogAction("Login", model.Email, "Successful login.", false);

                var token = await GenerateJwtToken(user);
                return Ok(new { Token = token });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error during login");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpPost("create-role")]
        [Authorize(Roles = "Administrator")]
        public async Task<IActionResult> CreateRole([FromBody] RoleDto roleDto)
        {
            if (string.IsNullOrWhiteSpace(roleDto.RoleName))
                return BadRequest("Role name is required.");

            try
            {
                var roleExists = await _roleManager.RoleExistsAsync(roleDto.RoleName);
                if (roleExists)
                    return BadRequest($"Role name {roleDto.RoleName} already exists");

                var result = await _roleManager.CreateAsync(new IdentityRole { Name = roleDto.RoleName });
                if (result.Succeeded)
                {
                    await _auditLogService.LogAction("Create Role", User.Identity?.Name ?? "Unknown", $"Created role: {roleDto.RoleName}", false);
                    return Ok();
                }

                foreach (var error in result.Errors)
                    ModelState.AddModelError(string.Empty, error.Description);

                return BadRequest(ModelState);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating role");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpPost("assign-role")]
        [Authorize(Roles = "Administrator")]
        public async Task<IActionResult> AddUserToRole([FromBody] RoleDto roleDto)
        {
            try
            {
                var user = await _userManager.FindByEmailAsync(roleDto.Email);
                if (user == null)
                    return NotFound($"User with email: {roleDto.Email} not found");

                var result = await _userManager.AddToRoleAsync(user, roleDto.RoleName);
                if (result.Succeeded)
                {
                    await _auditLogService.LogAction("Assign Role", User.Identity?.Name ?? "Unknown", $"Assigned role {roleDto.RoleName} to user {roleDto.Email}", false);
                    return Ok();
                }

                foreach (var error in result.Errors)
                    ModelState.AddModelError(string.Empty, error.Description);

                return BadRequest(ModelState);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error assigning role to user");
                return StatusCode(500, "Internal server error");
            }
        }

        /// <summary>Отримати дані поточного авторизованого користувача</summary>
        [HttpGet("me")]
        [Authorize(AuthenticationSchemes = JwtBearerDefaults.AuthenticationScheme)]
        public async Task<IActionResult> GetMe()
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (userId is null) return Unauthorized();

            var user = await _userManager.FindByIdAsync(userId);
            if (user is null) return NotFound();

            var roles = await _userManager.GetRolesAsync(user);

            return Ok(new
            {
                user.Id,
                user.UserName,
                user.Email,
                Roles = roles,
                user.OrganizationId
            });
        }

        private async Task<string> GenerateJwtToken(ApplicationUser user)
        {
            var tokenHandler = new JwtSecurityTokenHandler();
            var key = Encoding.UTF8.GetBytes(_configuration["Jwt:Key"] ?? string.Empty);
            var role = (await _userManager.GetRolesAsync(user)).FirstOrDefault();
            var expireDays = _configuration.GetValue<int>("Jwt:ExpireDays", 30);

            var tokenDescriptor = new SecurityTokenDescriptor
            {
                Subject = new ClaimsIdentity(new[]
                {
                    new Claim(ClaimTypes.NameIdentifier, user.Id ?? string.Empty),
                    new Claim(ClaimTypes.Name, user.UserName ?? string.Empty),
                    new Claim(ClaimTypes.Email, user.Email ?? string.Empty),
                    new Claim(ClaimTypes.Role, role ?? "User"),
                    new Claim("OrganizationId", user.OrganizationId ?? string.Empty)
                }),
                Expires = DateTime.UtcNow.AddDays(expireDays),
                SigningCredentials = new SigningCredentials(new SymmetricSecurityKey(key), SecurityAlgorithms.HmacSha256),
                Issuer = _configuration["Jwt:Issuer"],
                Audience = _configuration["Jwt:Audience"]
            };

            var token = tokenHandler.CreateToken(tokenDescriptor);
            return tokenHandler.WriteToken(token);
        }
    }
}
