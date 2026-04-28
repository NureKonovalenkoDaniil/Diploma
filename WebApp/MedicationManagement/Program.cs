using MedicationManagement.BackgroundServices;
using MedicationManagement.DBContext;
using MedicationManagement.Models;
using MedicationManagement.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Diagnostics;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using System.Text;

namespace MedicationManagement
{
    public class Program
    {
        public static async Task Main(string[] args)
        {
            var builder = WebApplication.CreateBuilder(args);

            builder.Logging.ClearProviders();
            builder.Logging.AddConsole();
            builder.Logging.AddDebug();

            RegisterServices(builder);
            ConfigureAuthentication(builder);
            ConfigureSwagger(builder);

            builder.Services.AddHostedService<ExpiryNotificationService>();
            builder.Services.AddHostedService<StorageConditionMonitoringService>();

            builder.WebHost.ConfigureKestrel(options =>
            {
                options.ListenAnyIP(5000);
            });

            var app = builder.Build();

            await SeedIdentityDataAsync(app);
            ConfigureMiddleware(app);

            // TD-15: single app.Run() call
            app.Run();
        }

        private static void RegisterServices(WebApplicationBuilder builder)
        {
            builder.Services.AddHttpContextAccessor();

            builder.Services.AddScoped<IServiceMedicine, ServiceMedicine>();
            builder.Services.AddScoped<IServiceStorageCondition, ServiceStorageCondition>();
            builder.Services.AddScoped<IServiceIoTDevice, ServiceIoTDevice>();
            builder.Services.AddScoped<IServiceAuditLog, ServiceAuditLog>();
            builder.Services.AddScoped<IServiceStorageLocation, ServiceStorageLocation>();
            builder.Services.AddScoped<IServiceStorageIncident, ServiceStorageIncident>();
            builder.Services.AddScoped<IServiceMedicineLifecycle, ServiceMedicineLifecycle>();
            builder.Services.AddScoped<IServiceNotification, ServiceNotification>();

            builder.Services.AddDbContext<MedicineStorageContext>(options =>
                options.UseSqlServer(builder.Configuration.GetConnectionString("DefaultConnection")));
            builder.Services.AddDbContext<UserContext>(options =>
                options.UseSqlServer(builder.Configuration.GetConnectionString("DefaultConnection")));

            builder.Services.AddIdentity<ApplicationUser, IdentityRole>(options =>
            {
                options.SignIn.RequireConfirmedEmail = true;
                options.Password.RequireDigit = false;
                options.Password.RequiredLength = 4;
                options.Password.RequireUppercase = false;
                options.Password.RequireLowercase = false;
                options.Password.RequiredUniqueChars = 0;
                options.Password.RequireNonAlphanumeric = false;
            })
            .AddEntityFrameworkStores<UserContext>()
            .AddDefaultTokenProviders();

            // Add CORS for SPA Frontend (Фаза 4)
            builder.Services.AddCors(options =>
            {
                options.AddPolicy("FrontendPolicy", policy =>
                {
                    policy.SetIsOriginAllowed(origin =>
                        {
                            var uri = new Uri(origin);
                            return uri.Host == "localhost" || uri.Host == "127.0.0.1";
                        })
                          .AllowAnyHeader()
                          .AllowAnyMethod()
                          .AllowCredentials();
                });
            });

            // TD-04: Ignore cyclic references to prevent 500 errors when serializing entity graphs (until full DTO mapping is introduced)
            builder.Services.AddControllers().AddNewtonsoftJson(options =>
            {
                options.SerializerSettings.ReferenceLoopHandling = Newtonsoft.Json.ReferenceLoopHandling.Ignore;
            });
            builder.Services.AddEndpointsApiExplorer();
        }

        // TD-14: Cookie auth removed - system uses JWT Bearer only
        private static void ConfigureAuthentication(WebApplicationBuilder builder)
        {
            builder.Services.AddAuthentication(options =>
            {
                options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
                options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
            })
            .AddJwtBearer(options =>
            {
                options.TokenValidationParameters = new TokenValidationParameters
                {
                    ValidateIssuer = true,
                    ValidateAudience = true,
                    ValidateLifetime = true,
                    ValidateIssuerSigningKey = true,
                    ValidIssuer = builder.Configuration["Jwt:Issuer"],
                    ValidAudience = builder.Configuration["Jwt:Audience"],
                    IssuerSigningKey = new SymmetricSecurityKey(
                        Encoding.UTF8.GetBytes(builder.Configuration["Jwt:Key"] ?? string.Empty))
                };

                options.Events = new JwtBearerEvents
                {
                    OnAuthenticationFailed = context =>
                    {
                        var logger = context.HttpContext.RequestServices
                            .GetRequiredService<ILogger<Program>>();
                        logger.LogError("Authentication failed: {Message}", context.Exception.Message);
                        return Task.CompletedTask;
                    },
                    OnTokenValidated = context =>
                    {
                        var logger = context.HttpContext.RequestServices
                            .GetRequiredService<ILogger<Program>>();
                        logger.LogInformation("Token validated successfully.");
                        return Task.CompletedTask;
                    }
                };
            });

            builder.Services.AddAuthorization();
        }

        private static void ConfigureSwagger(WebApplicationBuilder builder)
        {
            builder.Services.AddSwaggerGen(c =>
            {
                c.SwaggerDoc("v1", new OpenApiInfo
                {
                    Version = "v1",
                    Title = "Medication Management API",
                    Description = "API for managing medicines, storage conditions and IoT monitoring.",
                });
            });
        }

        private static async Task SeedIdentityDataAsync(WebApplication app)
        {
            using var scope = app.Services.CreateScope();
            var roleManager = scope.ServiceProvider.GetRequiredService<RoleManager<IdentityRole>>();
            var userManager = scope.ServiceProvider.GetRequiredService<UserManager<ApplicationUser>>();
            var config = scope.ServiceProvider.GetRequiredService<IConfiguration>();

            string[] roles = { "Administrator", "Manager", "User", "Device" };
            foreach (var role in roles)
            {
                if (!await roleManager.RoleExistsAsync(role))
                    await roleManager.CreateAsync(new IdentityRole(role));
            }

            var adminEmail = config["AdminSeeding:Email"];
            var adminPassword = config["AdminSeeding:Password"];

            if (!string.IsNullOrEmpty(adminEmail) && !string.IsNullOrEmpty(adminPassword))
            {
                var adminExists = await userManager.FindByEmailAsync(adminEmail);
                if (adminExists == null)
                {
                    var adminUser = new ApplicationUser
                    {
                        UserName = adminEmail,
                        Email = adminEmail,
                        EmailConfirmed = true,
                        OrganizationId = Guid.NewGuid().ToString() // Admin technically doesn't need one, but let's give him a unique one.
                    };
                    var result = await userManager.CreateAsync(adminUser, adminPassword);
                    if (result.Succeeded)
                    {
                        await userManager.AddToRoleAsync(adminUser, "Administrator");
                    }
                }
            }
        }

        private static void ConfigureMiddleware(WebApplication app)
        {
            // TD-13: Swagger enabled via appsettings.json Swagger:Enabled
            var swaggerEnabled = app.Configuration.GetValue<bool>("Swagger:Enabled", false);
            if (swaggerEnabled)
            {
                app.UseSwagger();
                app.UseSwaggerUI(c =>
                {
                    c.SwaggerEndpoint("/swagger/v1/swagger.json", "API v1");
                    c.RoutePrefix = "swagger";
                });
            }

            app.UseExceptionHandler("/error");
            app.Map("/error", (HttpContext context) =>
            {
                var error = context.Features.Get<IExceptionHandlerFeature>()?.Error;
                return Results.Problem(error?.Message);
            });

            app.UseRouting();

            // Застосування CORS політики
            app.UseCors("FrontendPolicy");

            app.UseAuthentication();
            app.UseAuthorization();

            app.Use(async (context, next) =>
            {
                if (context.Request.Path == "/" || context.Request.Path == "")
                {
                    context.Response.Redirect("/login.html", permanent: false);
                    return;
                }
                await next();
            });

            app.UseStaticFiles();
            app.MapControllers();
            // TD-15: app.Run() removed from here - called once in Main()
        }
    }
}
