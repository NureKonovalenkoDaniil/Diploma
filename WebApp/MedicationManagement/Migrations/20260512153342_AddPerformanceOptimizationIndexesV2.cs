using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace MedicationManagement.Migrations
{
    /// <inheritdoc />
    public partial class AddPerformanceOptimizationIndexesV2 : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_StorageIncidents_DeviceId",
                table: "StorageIncidents");

            migrationBuilder.DropIndex(
                name: "IX_StorageConditions_DeviceID",
                table: "StorageConditions");

            migrationBuilder.DropIndex(
                name: "IX_MedicineLifecycleEvents_MedicineId",
                table: "MedicineLifecycleEvents");

            migrationBuilder.CreateIndex(
                name: "IX_StorageLocations_OrganizationId",
                table: "StorageLocations",
                column: "OrganizationId");

            migrationBuilder.CreateIndex(
                name: "IX_StorageIncidents_DeviceId_OrganizationId",
                table: "StorageIncidents",
                columns: new[] { "DeviceId", "OrganizationId" });

            migrationBuilder.CreateIndex(
                name: "IX_StorageIncidents_OrganizationId_CreatedAt",
                table: "StorageIncidents",
                columns: new[] { "OrganizationId", "CreatedAt" },
                descending: new[] { false, true });

            migrationBuilder.CreateIndex(
                name: "IX_StorageIncidents_OrganizationId_Status",
                table: "StorageIncidents",
                columns: new[] { "OrganizationId", "Status" });

            migrationBuilder.CreateIndex(
                name: "IX_StorageConditions_DeviceID_Timestamp",
                table: "StorageConditions",
                columns: new[] { "DeviceID", "Timestamp" },
                descending: new[] { false, true });

            migrationBuilder.CreateIndex(
                name: "IX_StorageConditions_OrganizationId_Timestamp",
                table: "StorageConditions",
                columns: new[] { "OrganizationId", "Timestamp" },
                descending: new[] { false, true });

            migrationBuilder.CreateIndex(
                name: "IX_Notifications_OrganizationId_CreatedAt",
                table: "Notifications",
                columns: new[] { "OrganizationId", "CreatedAt" },
                descending: new[] { false, true });

            migrationBuilder.CreateIndex(
                name: "IX_Notifications_OrganizationId_IsRead",
                table: "Notifications",
                columns: new[] { "OrganizationId", "IsRead" });

            migrationBuilder.CreateIndex(
                name: "IX_Notifications_TargetRole",
                table: "Notifications",
                column: "TargetRole");

            migrationBuilder.CreateIndex(
                name: "IX_Medicines_OrganizationId_ExpiryDate",
                table: "Medicines",
                columns: new[] { "OrganizationId", "ExpiryDate" });

            migrationBuilder.CreateIndex(
                name: "IX_Medicines_OrganizationId_Status",
                table: "Medicines",
                columns: new[] { "OrganizationId", "Status" });

            migrationBuilder.CreateIndex(
                name: "IX_MedicineLifecycleEvents_MedicineId_OrganizationId",
                table: "MedicineLifecycleEvents",
                columns: new[] { "MedicineId", "OrganizationId" });

            migrationBuilder.CreateIndex(
                name: "IX_MedicineLifecycleEvents_OrganizationId",
                table: "MedicineLifecycleEvents",
                column: "OrganizationId");

            migrationBuilder.CreateIndex(
                name: "IX_IoTDevices_OrganizationId_IsActive",
                table: "IoTDevices",
                columns: new[] { "OrganizationId", "IsActive" });

            migrationBuilder.CreateIndex(
                name: "IX_AuditLogs_OrganizationId_Severity",
                table: "AuditLogs",
                columns: new[] { "OrganizationId", "Severity" });

            migrationBuilder.CreateIndex(
                name: "IX_AuditLogs_OrganizationId_Timestamp",
                table: "AuditLogs",
                columns: new[] { "OrganizationId", "Timestamp" },
                descending: new[] { false, true });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_StorageLocations_OrganizationId",
                table: "StorageLocations");

            migrationBuilder.DropIndex(
                name: "IX_StorageIncidents_DeviceId_OrganizationId",
                table: "StorageIncidents");

            migrationBuilder.DropIndex(
                name: "IX_StorageIncidents_OrganizationId_CreatedAt",
                table: "StorageIncidents");

            migrationBuilder.DropIndex(
                name: "IX_StorageIncidents_OrganizationId_Status",
                table: "StorageIncidents");

            migrationBuilder.DropIndex(
                name: "IX_StorageConditions_DeviceID_Timestamp",
                table: "StorageConditions");

            migrationBuilder.DropIndex(
                name: "IX_StorageConditions_OrganizationId_Timestamp",
                table: "StorageConditions");

            migrationBuilder.DropIndex(
                name: "IX_Notifications_OrganizationId_CreatedAt",
                table: "Notifications");

            migrationBuilder.DropIndex(
                name: "IX_Notifications_OrganizationId_IsRead",
                table: "Notifications");

            migrationBuilder.DropIndex(
                name: "IX_Notifications_TargetRole",
                table: "Notifications");

            migrationBuilder.DropIndex(
                name: "IX_Medicines_OrganizationId_ExpiryDate",
                table: "Medicines");

            migrationBuilder.DropIndex(
                name: "IX_Medicines_OrganizationId_Status",
                table: "Medicines");

            migrationBuilder.DropIndex(
                name: "IX_MedicineLifecycleEvents_MedicineId_OrganizationId",
                table: "MedicineLifecycleEvents");

            migrationBuilder.DropIndex(
                name: "IX_MedicineLifecycleEvents_OrganizationId",
                table: "MedicineLifecycleEvents");

            migrationBuilder.DropIndex(
                name: "IX_IoTDevices_OrganizationId_IsActive",
                table: "IoTDevices");

            migrationBuilder.DropIndex(
                name: "IX_AuditLogs_OrganizationId_Severity",
                table: "AuditLogs");

            migrationBuilder.DropIndex(
                name: "IX_AuditLogs_OrganizationId_Timestamp",
                table: "AuditLogs");

            migrationBuilder.CreateIndex(
                name: "IX_StorageIncidents_DeviceId",
                table: "StorageIncidents",
                column: "DeviceId");

            migrationBuilder.CreateIndex(
                name: "IX_StorageConditions_DeviceID",
                table: "StorageConditions",
                column: "DeviceID");

            migrationBuilder.CreateIndex(
                name: "IX_MedicineLifecycleEvents_MedicineId",
                table: "MedicineLifecycleEvents",
                column: "MedicineId");
        }
    }
}
