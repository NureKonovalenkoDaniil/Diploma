using NBomber.CSharp;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;

var httpClient = new HttpClient
{
    BaseAddress = new Uri("http://localhost:5001")
};

httpClient.DefaultRequestHeaders.Authorization =
    new AuthenticationHeaderValue("Bearer", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1laWQiOiJiYzA5YjNhOC00YzVhLTQ1YzUtOWVlOS03NjFhNmU4MDc0NmEiLCJ1bmlxdWVfbmFtZSI6ImFkbWluQG1lZHN0b3JhZ2UuY29tIiwiZW1haWwiOiJhZG1pbkBtZWRzdG9yYWdlLmNvbSIsInJvbGUiOiJBZG1pbmlzdHJhdG9yIiwiT3JnYW5pemF0aW9uSWQiOiJhOTVlNDYyYS1lYTA3LTQ5NzAtODZjMi0yOTExY2M1MzA4MzEiLCJuYmYiOjE3ODIxMzYzNjgsImV4cCI6MTc4NDcyODM2OCwiaWF0IjoxNzgyMTM2MzY4LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjUwMDEiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjUwMDEifQ.8sRU8P5lHcGVx31qGXpl5y-vTc0zgbkfanecT_BQydQ");

var random = new Random();

string GenerateMedicineJson()
{
    var medicine = new
    {
        Name = "TestMed_" + Guid.NewGuid().ToString("N").Substring(0, 6),
        Type = "Pill",
        ExpiryDate = DateTime.Now.AddDays(random.Next(30, 365)).ToString("yyyy-MM-dd"),
        Quantity = random.Next(1, 100),
        Category = "General"
    };

    return JsonSerializer.Serialize(medicine);
}

var scenario = Scenario.Create("POST /api/medicine", async context =>
{
    var json = GenerateMedicineJson();
    var content = new StringContent(json, Encoding.UTF8, "application/json");

    var response = await httpClient.PostAsync("/api/medicine", content);

    return response.IsSuccessStatusCode
        ? Response.Ok()
        : Response.Fail();
})
.WithWarmUpDuration(TimeSpan.FromSeconds(3))
.WithLoadSimulations(Simulation.KeepConstant(copies: 10, during: TimeSpan.FromSeconds(20)));

NBomberRunner
    .RegisterScenarios(scenario)
    .Run();
