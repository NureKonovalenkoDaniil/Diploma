using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace MedicationManagement.Migrations
{
    /// <inheritdoc />
    public partial class AddPerformanceOptimizationIndexes : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // --- Composite indexes for OrganizationId filtering (most common filter) ---

            // Medicines: Filter by OrganizationId, search by ExpiryDate, Status
            migrationBuilder.CreateIndex(
                name: "IX_Medicines_OrganizationId_ExpiryDate",
                table: "Medicines",
                columns: new[] { "OrganizationId", "ExpiryDate" });

            migrationBuilder.CreateIndex(
                name: "IX_Medicines_OrganizationId_Status",
                table: "Medicines",
                columns: new[] { "OrganizationId", "Status" });

            // Notifications: Filter by OrganizationId, sort by CreatedAt
            migrationBuilder.CreateIndex(
                name: "IX_Notifications_OrganizationId_CreatedAt",
                table: "Notifications",
                columns: new[] { "OrganizationId", "CreatedAt" },
                descending: new[] { false, true });

            migrationBuilder.CreateIndex(
                name: "IX_Notifications_OrganizationId_IsRead",
                table: "Notifications",
                columns: new[] { "OrganizationId", "IsRead" });

            // AuditLogs: Filter by OrganizationId, sort by Timestamp
            migrationBuilder.CreateIndex(
                name: "IX_AuditLogs_OrganizationId_Timestamp",
                table: "AuditLogs",
                columns: new[] { "OrganizationId", "Timestamp" },
                descending: new[] { false, true });

            migrationBuilder.CreateIndex(
                name: "IX_AuditLogs_OrganizationId_Severity",
                table: "AuditLogs",
                columns: new[] { "OrganizationId", "Severity" });

            // StorageIncidents: Filter by OrganizationId and Status
            migrationBuilder.CreateIndex(
                name: "IX_StorageIncidents_OrganizationId_Status",
                table: "StorageIncidents",
                columns: new[] { "OrganizationId", "Status" });

            migrationBuilder.CreateIndex(
                name: "IX_StorageIncidents_DeviceId_OrganizationId",
                table: "StorageIncidents",
                columns: new[] { "DeviceId", "OrganizationId" });

            migrationBuilder.CreateIndex(
                name: "IX_StorageIncidents_OrganizationId_CreatedAt",
                table: "StorageIncidents",
                columns: new[] { "OrganizationId", "CreatedAt" },
                descending: new[] { false, true });

            // StorageLocations: Filter by OrganizationId
            migrationBuilder.CreateIndex(
                name: "IX_StorageLocations_OrganizationId",
                table: "StorageLocations",
                column: "OrganizationId");

            // StorageConditions: Filter by OrganizationId and DeviceID, sort by Timestamp
            migrationBuilder.CreateIndex(
                name: "IX_StorageConditions_OrganizationId_Timestamp",
                table: "StorageConditions",
                columns: new[] { "OrganizationId", "Timestamp" },
                descending: new[] { false, true });

            migrationBuilder.CreateIndex(
                name: "IX_StorageConditions_DeviceID_Timestamp",
                table: "StorageConditions",
                columns: new[] { "DeviceID", "Timestamp" },
                descending: new[] { false, true });

            // MedicineLifecycleEvents: Filter by MedicineId and OrganizationId
            migrationBuilder.CreateIndex(
                name: "IX_MedicineLifecycleEvents_OrganizationId",
                table: "MedicineLifecycleEvents",
                column: "OrganizationId");

            migrationBuilder.CreateIndex(
                name: "IX_MedicineLifecycleEvents_MedicineId_OrganizationId",
                table: "MedicineLifecycleEvents",
                columns: new[] { "MedicineId", "OrganizationId" });

            // IoTDevices: Filter by OrganizationId and IsActive
            migrationBuilder.CreateIndex(
                name: "IX_IoTDevices_OrganizationId_IsActive",
                table: "IoTDevices",
                columns: new[] { "OrganizationId", "IsActive" });

            // Notifications: Index for TargetRole filtering (for role-based notifications)
            migrationBuilder.CreateIndex(
                name: "IX_Notifications_TargetRole",
                table: "Notifications",
                column: "TargetRole");

            // Note: EntityType in AuditLogs is nullable nvarchar(max), which cannot be indexed in SQL Server
            // Filtering by EntityType is less critical and can be done via application-level filtering if needed
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_Medicines_OrganizationId_ExpiryDate",
                table: "Medicines");

            migrationBuilder.DropIndex(
                name: "IX_Medicines_OrganizationId_Status",
                table: "Medicines");

            migrationBuilder.DropIndex(
                name: "IX_Notifications_OrganizationId_CreatedAt",
                table: "Notifications");

            migrationBuilder.DropIndex(
                name: "IX_Notifications_OrganizationId_IsRead",
                table: "Notifications");

            migrationBuilder.DropIndex(
                name: "IX_AuditLogs_OrganizationId_Timestamp",
                table: "AuditLogs");

            migrationBuilder.DropIndex(
                name: "IX_AuditLogs_OrganizationId_Severity",
                table: "AuditLogs");

            migrationBuilder.DropIndex(
                name: "IX_StorageIncidents_OrganizationId_Status",
                table: "StorageIncidents");

            migrationBuilder.DropIndex(
                name: "IX_StorageIncidents_DeviceId_OrganizationId",
                table: "StorageIncidents");

            migrationBuilder.DropIndex(
                name: "IX_StorageIncidents_OrganizationId_CreatedAt",
                table: "StorageIncidents");

            migrationBuilder.DropIndex(
                name: "IX_StorageLocations_OrganizationId",
                table: "StorageLocations");

            migrationBuilder.DropIndex(
                name: "IX_StorageConditions_OrganizationId_Timestamp",
                table: "StorageConditions");

            migrationBuilder.DropIndex(
                name: "IX_StorageConditions_DeviceID_Timestamp",
                table: "StorageConditions");

            migrationBuilder.DropIndex(
                name: "IX_MedicineLifecycleEvents_OrganizationId",
                table: "MedicineLifecycleEvents");

            migrationBuilder.DropIndex(
                name: "IX_MedicineLifecycleEvents_MedicineId_OrganizationId",
                table: "MedicineLifecycleEvents");

            migrationBuilder.DropIndex(
                name: "IX_IoTDevices_OrganizationId_IsActive",
                table: "IoTDevices");

            migrationBuilder.DropIndex(
                name: "IX_Notifications_TargetRole",
                table: "Notifications");
        }
    }
}
