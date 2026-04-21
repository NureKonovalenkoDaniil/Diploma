namespace MedicationManagement.Models.DTOs
{
    /// <summary>
    /// DTO для операцій з ролями (створення ролі, призначення ролі користувачу).
    /// Використовується у POST /api/auth/create-role та POST /api/auth/assign-role.
    /// </summary>
    public class RoleDto
    {
        /// <summary>Email користувача (для assign-role)</summary>
        public string Email { get; set; } = string.Empty;

        /// <summary>Назва ролі (обов'язково)</summary>
        public string RoleName { get; set; } = string.Empty;
    }
}
