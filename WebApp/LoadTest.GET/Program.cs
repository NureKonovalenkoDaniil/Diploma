using NBomber.CSharp;

using var httpClient = new HttpClient
{
    BaseAddress = new Uri("https://localhost:7069")
};

httpClient.DefaultRequestHeaders.Authorization =
    new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1laWQiOiI2OGE2N2I1Mi1jZjc3LTQwY2ItYjk0Yy05MTAyYjlmOGZlOTYiLCJ1bmlxdWVfbmFtZSI6ImFkbWluQGdtYWlsLmNvbSIsImVtYWlsIjoiYWRtaW5AZ21haWwuY29tIiwicm9sZSI6IkFkbWluaXN0cmF0b3IiLCJuYmYiOjE3NDg2MTI1MDgsImV4cCI6MTc4MDE0ODUwOCwiaWF0IjoxNzQ4NjEyNTA4LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjcwNjkiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjcwNjkifQ.fQJRmaCU8TkjI3g63VCq68PSjBNtUVY43mLtmFd_FPI");

var scenario = Scenario.Create("GET /api/medicine", async context =>
{
    var response = await httpClient.GetAsync("/api/medicine");

    return response.IsSuccessStatusCode
        ? Response.Ok()
        : Response.Fail();
})
.WithWarmUpDuration(TimeSpan.FromSeconds(5))
.WithLoadSimulations(Simulation.KeepConstant(copies: 50, during: TimeSpan.FromSeconds(15)));

NBomberRunner
    .RegisterScenarios(scenario)
    .Run();
