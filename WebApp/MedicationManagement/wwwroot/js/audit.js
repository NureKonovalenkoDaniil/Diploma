document.addEventListener('DOMContentLoaded', () => {
  const token = localStorage.getItem('token');
  const tableBody = document.querySelector('#auditTable tbody');
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
    document.querySelectorAll('#auditTable tbody tr').forEach((row) => {
      row.style.display = row.innerText.toLowerCase().includes(query) ? '' : 'none';
    });
  }

  async function loadAuditLogs() {
    try {
      const res = await fetch('/api/auditlog', {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!res.ok) throw new Error('Failed to fetch logs');

      const data = await res.json();
      tableBody.innerHTML = '';

      data.forEach((entry) => {
        const row = document.createElement('tr');
        row.innerHTML = `
          <td>${entry.action}</td>
          <td>${entry.user}</td>
          <td>${new Date(entry.timestamp).toLocaleString()}</td>
          <td>${entry.details || '-'}</td>
        `;
        tableBody.appendChild(row);
      });

      applyFilter();
    } catch (err) {
      messageBox.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
    }
  }

  loadAuditLogs();
  setInterval(loadAuditLogs, 5000); // Автооновлення кожні 5 сек
});
