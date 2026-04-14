using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace MedicationManagement.Migrations
{
    /// <inheritdoc />
    public partial class DiplomaPhase2_DomainModel : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "BatchNumber",
                table: "Medicines",
                type: "nvarchar(50)",
                maxLength: 50,
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "Description",
                table: "Medicines",
                type: "nvarchar(max)",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "Manufacturer",
                table: "Medicines",
                type: "nvarchar(100)",
                maxLength: 100,
                nullable: true);

            migrationBuilder.AddColumn<float>(
                name: "MaxStorageTemp",
                table: "Medicines",
                type: "real",
                nullable: true);

            migrationBuilder.AddColumn<float>(
                name: "MinStorageTemp",
                table: "Medicines",
                type: "real",
                nullable: true);

            migrationBuilder.AddColumn<int>(
                name: "StorageLocationId",
                table: "Medicines",
                type: "int",
                nullable: true);

            migrationBuilder.AddColumn<int>(
                name: "EntityId",
                table: "AuditLogs",
                type: "int",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "EntityType",
                table: "AuditLogs",
                type: "nvarchar(max)",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "Severity",
                table: "AuditLogs",
                type: "nvarchar(20)",
                maxLength: 20,
                nullable: false,
                defaultValue: "");

            migrationBuilder.CreateTable(
                name: "Notifications",
                columns: table => new
                {
                    NotificationId = table.Column<int>(type: "int", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    Type = table.Column<string>(type: "nvarchar(50)", maxLength: 50, nullable: false),
                    Title = table.Column<string>(type: "nvarchar(200)", maxLength: 200, nullable: false),
                    Message = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    TargetRole = table.Column<string>(type: "nvarchar(50)", maxLength: 50, nullable: false),
                    IsRead = table.Column<bool>(type: "bit", nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "datetime2", nullable: false),
                    RelatedEntityType = table.Column<string>(type: "nvarchar(50)", maxLength: 50, nullable: true),
                    RelatedEntityId = table.Column<int>(type: "int", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Notifications", x => x.NotificationId);
                });

            migrationBuilder.CreateTable(
                name: "StorageLocations",
                columns: table => new
                {
                    LocationId = table.Column<int>(type: "int", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    Name = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: false),
                    Address = table.Column<string>(type: "nvarchar(250)", maxLength: 250, nullable: true),
                    LocationType = table.Column<string>(type: "nvarchar(50)", maxLength: 50, nullable: false),
                    IoTDeviceId = table.Column<int>(type: "int", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_StorageLocations", x => x.LocationId);
                    table.ForeignKey(
                        name: "FK_StorageLocations_IoTDevices_IoTDeviceId",
                        column: x => x.IoTDeviceId,
                        principalTable: "IoTDevices",
                        principalColumn: "DeviceID",
                        onDelete: ReferentialAction.SetNull);
                });

            migrationBuilder.CreateTable(
                name: "MedicineLifecycleEvents",
                columns: table => new
                {
                    EventId = table.Column<int>(type: "int", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    MedicineId = table.Column<int>(type: "int", nullable: false),
                    EventType = table.Column<string>(type: "nvarchar(50)", maxLength: 50, nullable: false),
                    Description = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    Quantity = table.Column<int>(type: "int", nullable: true),
                    PerformedBy = table.Column<string>(type: "nvarchar(256)", maxLength: 256, nullable: false),
                    PerformedAt = table.Column<DateTime>(type: "datetime2", nullable: false),
                    RelatedLocationId = table.Column<int>(type: "int", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_MedicineLifecycleEvents", x => x.EventId);
                    table.ForeignKey(
                        name: "FK_MedicineLifecycleEvents_Medicines_MedicineId",
                        column: x => x.MedicineId,
                        principalTable: "Medicines",
                        principalColumn: "MedicineID",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_MedicineLifecycleEvents_StorageLocations_RelatedLocationId",
                        column: x => x.RelatedLocationId,
                        principalTable: "StorageLocations",
                        principalColumn: "LocationId",
                        onDelete: ReferentialAction.SetNull);
                });

            migrationBuilder.CreateTable(
                name: "StorageIncidents",
                columns: table => new
                {
                    IncidentId = table.Column<int>(type: "int", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    DeviceId = table.Column<int>(type: "int", nullable: false),
                    LocationId = table.Column<int>(type: "int", nullable: true),
                    IncidentType = table.Column<string>(type: "nvarchar(50)", maxLength: 50, nullable: false),
                    DetectedValue = table.Column<float>(type: "real", nullable: false),
                    ExpectedMin = table.Column<float>(type: "real", nullable: false),
                    ExpectedMax = table.Column<float>(type: "real", nullable: false),
                    StartTime = table.Column<DateTime>(type: "datetime2", nullable: false),
                    EndTime = table.Column<DateTime>(type: "datetime2", nullable: true),
                    Status = table.Column<string>(type: "nvarchar(20)", maxLength: 20, nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_StorageIncidents", x => x.IncidentId);
                    table.ForeignKey(
                        name: "FK_StorageIncidents_IoTDevices_DeviceId",
                        column: x => x.DeviceId,
                        principalTable: "IoTDevices",
                        principalColumn: "DeviceID",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_StorageIncidents_StorageLocations_LocationId",
                        column: x => x.LocationId,
                        principalTable: "StorageLocations",
                        principalColumn: "LocationId",
                        onDelete: ReferentialAction.SetNull);
                });

            migrationBuilder.CreateIndex(
                name: "IX_Medicines_StorageLocationId",
                table: "Medicines",
                column: "StorageLocationId");

            migrationBuilder.CreateIndex(
                name: "IX_MedicineLifecycleEvents_MedicineId",
                table: "MedicineLifecycleEvents",
                column: "MedicineId");

            migrationBuilder.CreateIndex(
                name: "IX_MedicineLifecycleEvents_RelatedLocationId",
                table: "MedicineLifecycleEvents",
                column: "RelatedLocationId");

            migrationBuilder.CreateIndex(
                name: "IX_StorageIncidents_DeviceId",
                table: "StorageIncidents",
                column: "DeviceId");

            migrationBuilder.CreateIndex(
                name: "IX_StorageIncidents_LocationId",
                table: "StorageIncidents",
                column: "LocationId");

            migrationBuilder.CreateIndex(
                name: "IX_StorageLocations_IoTDeviceId",
                table: "StorageLocations",
                column: "IoTDeviceId");

            migrationBuilder.AddForeignKey(
                name: "FK_Medicines_StorageLocations_StorageLocationId",
                table: "Medicines",
                column: "StorageLocationId",
                principalTable: "StorageLocations",
                principalColumn: "LocationId",
                onDelete: ReferentialAction.SetNull);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_Medicines_StorageLocations_StorageLocationId",
                table: "Medicines");

            migrationBuilder.DropTable(
                name: "MedicineLifecycleEvents");

            migrationBuilder.DropTable(
                name: "Notifications");

            migrationBuilder.DropTable(
                name: "StorageIncidents");

            migrationBuilder.DropTable(
                name: "StorageLocations");

            migrationBuilder.DropIndex(
                name: "IX_Medicines_StorageLocationId",
                table: "Medicines");

            migrationBuilder.DropColumn(
                name: "BatchNumber",
                table: "Medicines");

            migrationBuilder.DropColumn(
                name: "Description",
                table: "Medicines");

            migrationBuilder.DropColumn(
                name: "Manufacturer",
                table: "Medicines");

            migrationBuilder.DropColumn(
                name: "MaxStorageTemp",
                table: "Medicines");

            migrationBuilder.DropColumn(
                name: "MinStorageTemp",
                table: "Medicines");

            migrationBuilder.DropColumn(
                name: "StorageLocationId",
                table: "Medicines");

            migrationBuilder.DropColumn(
                name: "EntityId",
                table: "AuditLogs");

            migrationBuilder.DropColumn(
                name: "EntityType",
                table: "AuditLogs");

            migrationBuilder.DropColumn(
                name: "Severity",
                table: "AuditLogs");
        }
    }
}
