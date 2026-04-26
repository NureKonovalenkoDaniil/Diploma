using MedicationManagement.Models;
using MedicationManagement.Models.DTOs;
using MedicationManagement.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace MedicationManagement.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    [Authorize(AuthenticationSchemes = JwtBearerDefaults.AuthenticationScheme)]
    public class NotificationController : ControllerBase
    {
        private readonly IServiceNotification _notificationService;

        public NotificationController(IServiceNotification notificationService)
        {
            _notificationService = notificationService;
        }

        /// <summary>Отримати всі сповіщення (з optional фільтрацією по ролі)</summary>
        [HttpGet]
        public async Task<IActionResult> GetAll([FromQuery] string? role = null)
        {
            var notifications = await _notificationService.GetAll(role);
            return Ok(notifications.Select(n => n.ToDto()));
        }

        /// <summary>Отримати непрочитані сповіщення</summary>
        [HttpGet("unread")]
        public async Task<IActionResult> GetUnread([FromQuery] string? role = null)
        {
            var notifications = await _notificationService.GetUnread(role);
            return Ok(notifications.Select(n => n.ToDto()));
        }

        /// <summary>Створити сповіщення (тільки Administrator)</summary>
        [HttpPost]
        [Authorize(Roles = "Administrator")]
        public async Task<IActionResult> Create([FromBody] CreateNotificationDto dto)
        {
            if (!ModelState.IsValid) return BadRequest(ModelState);

            var created = await _notificationService.Create(
                dto.Type, 
                dto.Title, 
                dto.Message, 
                dto.TargetRole, 
                dto.RelatedEntityType, 
                dto.RelatedEntityId);

            return Ok(created.ToDto());
        }

        /// <summary>Позначити сповіщення як прочитане</summary>
        [HttpPatch("{id}/read")]
        public async Task<IActionResult> MarkAsRead(int id)
        {
            var success = await _notificationService.MarkAsRead(id);
            if (!success) return NotFound();
            return NoContent();
        }

        /// <summary>Позначити всі сповіщення як прочитані</summary>
        [HttpPatch("read-all")]
        public async Task<IActionResult> MarkAllAsRead([FromQuery] string? role = null)
        {
            var count = await _notificationService.MarkAllAsRead(role);
            return Ok(new { markedAsRead = count });
        }
    }
}
