using System.Net;
using System.Net.Mail;
using Azure;
using Azure.Communication.Email;
using MedicationManagement.Models;
using Microsoft.Extensions.Options;

namespace MedicationManagement.Services
{
    public class EmailSender : IEmailSender
    {
        private readonly EmailSettings _settings;
        private readonly ILogger<EmailSender> _logger;

        public EmailSender(IOptions<EmailSettings> options, ILogger<EmailSender> logger)
        {
            _settings = options.Value;
            _logger = logger;
        }

        public async Task SendAsync(string toEmail, string subject, string htmlBody)
        {
            if (!string.IsNullOrWhiteSpace(_settings.AzureConnectionString))
            {
                await SendViaAzureAsync(toEmail, subject, htmlBody);
            }
            else
            {
                await SendViaSmtpAsync(toEmail, subject, htmlBody);
            }
        }

        private async Task SendViaAzureAsync(string toEmail, string subject, string htmlBody)
        {
            try
            {
                _logger.LogInformation("Sending email to {Email} via Azure Communication Services...", toEmail);
                var emailClient = new EmailClient(_settings.AzureConnectionString);
                
                // В Azure ACS FromEmail має бути зареєстрований домен відправника.
                var senderAddress = _settings.FromEmail;
                
                var emailSendOperation = await emailClient.SendAsync(
                    WaitUntil.Completed,
                    senderAddress: senderAddress,
                    recipientAddress: toEmail,
                    subject: subject,
                    htmlContent: htmlBody);

                _logger.LogInformation("Email sent successfully to {Email} via Azure. OperationId: {OperationId}", 
                    toEmail, emailSendOperation.Id);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to send email to {Email} via Azure Communication Services", toEmail);
            }
        }

        private async Task SendViaSmtpAsync(string toEmail, string subject, string htmlBody)
        {
            if (string.IsNullOrWhiteSpace(_settings.SmtpHost) || string.IsNullOrWhiteSpace(_settings.FromEmail))
            {
                _logger.LogWarning("Email settings (SMTP/Azure) are not configured. Skipping email send to {Email}", toEmail);
                return;
            }

            _logger.LogInformation("Sending email to {Email} via SMTP ({Host})...", toEmail, _settings.SmtpHost);

            using var message = new MailMessage
            {
                From = new MailAddress(_settings.FromEmail, _settings.FromName),
                Subject = subject,
                Body = htmlBody,
                IsBodyHtml = true
            };

            message.To.Add(new MailAddress(toEmail));

            using var client = new SmtpClient(_settings.SmtpHost, _settings.SmtpPort)
            {
                EnableSsl = _settings.UseSsl,
                Credentials = string.IsNullOrWhiteSpace(_settings.SmtpUser)
                    ? CredentialCache.DefaultNetworkCredentials
                    : new NetworkCredential(_settings.SmtpUser, _settings.SmtpPass)
            };

            try
            {
                await client.SendMailAsync(message);
                _logger.LogInformation("Email sent successfully to {Email} via SMTP.", toEmail);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to send email to {Email} via SMTP with subject '{Subject}'", toEmail, subject);
            }
        }
    }
}
