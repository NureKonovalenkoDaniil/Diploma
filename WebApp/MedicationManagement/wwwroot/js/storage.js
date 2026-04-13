document.addEventListener('DOMContentLoaded', () => {
  const token = localStorage.getItem('token');
  const tableBody = document.querySelector('#storageTable tbody');
  const messageBox = document.getElementById('message');
  const logoutBtn = document.getElementById('logoutBtn');

  if (!token) {
    window.location.href = 'login.html';
    return;
  }

  logoutBtn.addEventListener('click', () => {
    localStorage.removeItem('token');
    window.location.href = 'login.html';
  });

  function applyFilter() {
    const query = searchInput.value.toLowerCase();
    document.querySelectorAll('#storageTable tbody tr').forEach((row) => {
      row.style.display = row.innerText.toLowerCase().includes(query) ? '' : 'none';
    });
  }

  async function loadStorageConditions() {
    try {
      const res = await fetch('/api/storagecondition', {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();

      tableBody.innerHTML = '';
      data.forEach((entry) => {
        const row = document.createElement('tr');
        row.innerHTML = `
          <td>${entry.deviceID}</td>
          <td>${entry.temperature.toFixed(1)} °C</td>
          <td>${entry.humidity.toFixed(1)} %</td>
          <td>${new Date(entry.timestamp).toLocaleString()}</td>
        `;
        tableBody.appendChild(row);
      });

      applyFilter();
    } catch (err) {
      messageBox.innerHTML = `<div class="alert alert-danger">Error loading storage data</div>`;
    }
  }

  loadStorageConditions();
  setInterval(loadStorageConditions, 5000);
});
