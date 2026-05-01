using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace MedicationManagement.Migrations
{
    /// <inheritdoc />
    public partial class AddMedicineHumidity : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<float>(
                name: "MaxStorageHumidity",
                table: "Medicines",
                type: "real",
                nullable: true);

            migrationBuilder.AddColumn<float>(
                name: "MinStorageHumidity",
                table: "Medicines",
                type: "real",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "MaxStorageHumidity",
                table: "Medicines");

            migrationBuilder.DropColumn(
                name: "MinStorageHumidity",
                table: "Medicines");
        }
    }
}
