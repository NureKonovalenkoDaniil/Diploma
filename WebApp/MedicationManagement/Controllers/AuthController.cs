using MedicationManagement.Models;
using MedicationManagement.Models.DTOs;
using MedicationManagement.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.WebUtilities;
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
        private readonly IServiceIoTDevice _ioTDeviceService;
        private readonly IEmailSender _emailSender;

        public AuthController(UserManager<ApplicationUser> userManager,
                              SignInManager<ApplicationUser> signInManager,
                              RoleManager<IdentityRole> roleManager,
                              IConfiguration configuration,
                              ILogger<AuthController> logger,
                              IServiceAuditLog auditLogService,
                              IServiceIoTDevice ioTDeviceService,
                              IEmailSender emailSender)
        {
            _userManager = userManager;
            _signInManager = signInManager;
            _roleManager = roleManager;
            _configuration = configuration;
            _logger = logger;
            _auditLogService = auditLogService;
            _ioTDeviceService = ioTDeviceService;
            _emailSender = emailSender;
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

                var user = new ApplicationUser
                {
                    UserName = model.Email,
                    Email = model.Email,
                    EmailConfirmed = false,
                    SecurityStamp = Guid.NewGuid().ToString(),
                    OrganizationId = Guid.NewGuid().ToString()
                };

                var result = await _userManager.CreateAsync(user, model.Password);
                if (!result.Succeeded)
                    return BadRequest(result.Errors);

                await _userManager.AddToRoleAsync(user, "User");
                await SendEmailConfirmationAsync(user);

                await _auditLogService.LogAction("Register", model.Email, "Registered new user with role User.", false);

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

                if (!user.EmailConfirmed)
                    return StatusCode(403, "Email is not confirmed");

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

        [HttpPost("create-manager")]
        [Authorize(Roles = "Administrator")]
        public async Task<IActionResult> CreateManager([FromBody] CreateManagerDto model)
        {
            if (!ModelState.IsValid)
                return BadRequest(ModelState);

            try
            {
                var userExists = await _userManager.FindByEmailAsync(model.Email);
                if (userExists != null)
                    return Conflict("User already exists!");

                var user = new ApplicationUser
                {
                    UserName = model.Email,
                    Email = model.Email,
                    EmailConfirmed = false,
                    SecurityStamp = Guid.NewGuid().ToString(),
                    OrganizationId = model.OrganizationId
                };

                var result = await _userManager.CreateAsync(user, model.Password);
                if (!result.Succeeded)
                    return BadRequest(result.Errors);

                await _userManager.AddToRoleAsync(user, "Manager");
                await SendEmailConfirmationAsync(user);

                await _auditLogService.LogAction("CreateManager", User.Identity?.Name ?? "Unknown", $"Created manager {model.Email} for org {model.OrganizationId}.", false);

                return Ok("Manager created successfully!");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating manager");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpGet("confirm-email")]
        public async Task<IActionResult> ConfirmEmail([FromQuery] string userId, [FromQuery] string token)
        {
            if (string.IsNullOrWhiteSpace(userId) || string.IsNullOrWhiteSpace(token))
                return BadRequest("Invalid confirmation data");

            var user = await _userManager.FindByIdAsync(userId);
            if (user is null) return BadRequest("Invalid user");

            var decodedToken = Encoding.UTF8.GetString(WebEncoders.Base64UrlDecode(token));
            var result = await _userManager.ConfirmEmailAsync(user, decodedToken);
            if (!result.Succeeded)
                return BadRequest("Email confirmation failed");

            await _auditLogService.LogAction("ConfirmEmail", user.Email ?? "Unknown", "Email confirmed.", false);
            return Ok("Email confirmed successfully");
        }

        [HttpPost("resend-confirmation")]
        public async Task<IActionResult> ResendConfirmation([FromBody] ResendConfirmationDto model)
        {
            if (!ModelState.IsValid)
                return BadRequest(ModelState);

            var user = await _userManager.FindByEmailAsync(model.Email);
            if (user is null)
                return Ok("If the account exists, a confirmation email was sent.");

            if (user.EmailConfirmed)
                return Ok("Email is already confirmed.");

            await SendEmailConfirmationAsync(user);
            return Ok("Confirmation email sent.");
        }

        [HttpPost("forgot-password")]
        public async Task<IActionResult> ForgotPassword([FromBody] ForgotPasswordDto model)
        {
            if (!ModelState.IsValid)
                return BadRequest(ModelState);

            var user = await _userManager.FindByEmailAsync(model.Email);
            if (user == null || !user.EmailConfirmed)
                return Ok("If the account exists, a reset email was sent.");

            await SendPasswordResetAsync(user);
            return Ok("Reset email sent.");
        }

        [HttpPost("reset-password")]
        public async Task<IActionResult> ResetPassword([FromBody] ResetPasswordDto model)
        {
            if (!ModelState.IsValid)
                return BadRequest(ModelState);

            var user = await _userManager.FindByEmailAsync(model.Email);
            if (user == null)
                return BadRequest("Invalid reset request");

            var decodedToken = Encoding.UTF8.GetString(WebEncoders.Base64UrlDecode(model.Token));
            var result = await _userManager.ResetPasswordAsync(user, decodedToken, model.NewPassword);
            if (!result.Succeeded)
                return BadRequest(result.Errors);

            await _auditLogService.LogAction("ResetPassword", user.Email ?? "Unknown", "Password reset completed.", false);
            return Ok("Password reset successful");
        }

        [HttpPost("device-login")]
        public async Task<IActionResult> DeviceLogin([FromBody] DeviceLoginDto model)
        {
            try
            {
                var device = await _ioTDeviceService.ValidateDeviceSecret(model.DeviceId, model.DeviceSecret);
                if (device == null)
                    return Unauthorized("Invalid device credentials");

                await _auditLogService.LogAction("DeviceLogin", $"Device-{model.DeviceId}", "Successful device login.", false);

                var token = GenerateJwtTokenForDevice(device);
                return Ok(new { Token = token });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error during device login");
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

        /// <summary>Отримати список усіх користувачів (тільки Administrator)</summary>
        [HttpGet("users")]
        [Authorize(Roles = "Administrator", AuthenticationSchemes = JwtBearerDefaults.AuthenticationScheme)]
        public async Task<IActionResult> GetUsers()
        {
            try
            {
                var adminOrgId = User.FindFirst("OrganizationId")?.Value;
                var users = await _userManager.Users
                    .Where(u => u.OrganizationId == adminOrgId)
                    .ToListAsync();

                var result = new List<object>();
                foreach (var u in users)
                {
                    var roles = await _userManager.GetRolesAsync(u);
                    result.Add(new
                    {
                        u.Id,
                        u.Email,
                        u.UserName,
                        Roles = roles,
                        u.OrganizationId
                    });
                }

                return Ok(result);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error getting users");
                return StatusCode(500, "Internal server error");
            }
        }

        /// <summary>Видалити користувача (тільки Administrator)</summary>
        [HttpDelete("users/{id}")]
        [Authorize(Roles = "Administrator", AuthenticationSchemes = JwtBearerDefaults.AuthenticationScheme)]
        public async Task<IActionResult> DeleteUser(string id)
        {
            try
            {
                var adminOrgId = User.FindFirst("OrganizationId")?.Value;
                var user = await _userManager.FindByIdAsync(id);
                if (user == null) return NotFound();
                if (user.OrganizationId != adminOrgId) return Forbid();

                var roles = await _userManager.GetRolesAsync(user);
                if (roles.Contains("Administrator")) return BadRequest("Неможливо видалити адміністратора.");

                var result = await _userManager.DeleteAsync(user);
                if (!result.Succeeded) return BadRequest(result.Errors);

                await _auditLogService.LogAction("DeleteUser", User.Identity?.Name ?? "Unknown",
                    $"Deleted user {user.Email}.", false);

                return NoContent();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error deleting user");
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

        private string GenerateJwtTokenForDevice(IoTDevice device)
        {
            var tokenHandler = new JwtSecurityTokenHandler();
            var key = Encoding.UTF8.GetBytes(_configuration["Jwt:Key"] ?? string.Empty);
            var expireDays = _configuration.GetValue<int>("Jwt:ExpireDays", 30);

            var tokenDescriptor = new SecurityTokenDescriptor
            {
                Subject = new ClaimsIdentity(new[]
                {
                    new Claim(ClaimTypes.NameIdentifier, device.DeviceID),
                    new Claim(ClaimTypes.Name, $"Device-{device.DeviceID}"),
                    new Claim(ClaimTypes.Role, "Device"),
                    new Claim("OrganizationId", device.OrganizationId ?? string.Empty)
                }),
                Expires = DateTime.UtcNow.AddDays(expireDays),
                SigningCredentials = new SigningCredentials(new SymmetricSecurityKey(key), SecurityAlgorithms.HmacSha256),
                Issuer = _configuration["Jwt:Issuer"],
                Audience = _configuration["Jwt:Audience"]
            };

            var token = tokenHandler.CreateToken(tokenDescriptor);
            return tokenHandler.WriteToken(token);
        }

        private async Task SendEmailConfirmationAsync(ApplicationUser user)
        {
            var token = await _userManager.GenerateEmailConfirmationTokenAsync(user);
            var encodedToken = WebEncoders.Base64UrlEncode(Encoding.UTF8.GetBytes(token));

            var baseUrl = _configuration["Frontend:BaseUrl"] ?? "http://localhost:5173";
            var link = $"{baseUrl.TrimEnd('/')}/confirm-email?userId={Uri.EscapeDataString(user.Id)}&token={Uri.EscapeDataString(encodedToken)}";

            var subject = "Підтвердження email";
            var body = $"<p>Підтвердіть вашу адресу електронної пошти, натиснувши на посилання:</p><p><a href=\"{link}\">Підтвердити email</a></p>";

            await _emailSender.SendAsync(user.Email ?? string.Empty, subject, body);
        }

        private async Task SendPasswordResetAsync(ApplicationUser user)
        {
            var token = await _userManager.GeneratePasswordResetTokenAsync(user);
            var encodedToken = WebEncoders.Base64UrlEncode(Encoding.UTF8.GetBytes(token));

            var baseUrl = _configuration["Frontend:BaseUrl"] ?? "http://localhost:5173";
            var link = $"{baseUrl.TrimEnd('/')}/reset-password?email={Uri.EscapeDataString(user.Email ?? string.Empty)}&token={Uri.EscapeDataString(encodedToken)}";

            var subject = "Відновлення пароля";
            var body = $"<p>Для відновлення пароля перейдіть за посиланням:</p><p><a href=\"{link}\">Скинути пароль</a></p>";

            await _emailSender.SendAsync(user.Email ?? string.Empty, subject, body);
        }
    }
}
