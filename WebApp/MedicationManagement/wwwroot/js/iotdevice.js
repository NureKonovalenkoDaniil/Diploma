document.addEventListener('DOMContentLoaded', () => {
  const token = localStorage.getItem('token');
  const tableBody = document.querySelector('#iotTable tbody');
  const messageBox = document.getElementById('message');
  const form = document.getElementById('iotForm');
  const logoutBtn = document.getElementById('logoutBtn');

  if (!token) {
    window.location.href = 'login.html';
    return;
  }

  logoutBtn.addEventListener('click', () => {
    localStorage.removeItem('token');
    window.location.href = 'login.html';
  });

  async function loadDevices() {
    tableBody.innerHTML = '';
    try {
      const response = await fetch('/api/iotdevice', {
        headers: { Authorization: `Bearer ${token}` },
      });
      const devices = await response.json();
      devices.forEach((dev) => {
        const row = document.createElement('tr');
        row.innerHTML = `
          <td>${dev.deviceID}</td>
          <td>${dev.type}</td>
          <td>${dev.location}</td>
          <td>${dev.parameters}</td>
          <td>${dev.isActive ? 'Yes' : 'No'}</td>
          <td>
            <button class="btn btn-danger btn-sm me-1" onclick="confirmDelete(${
              dev.deviceID
            })">Delete</button>
            <button class="btn btn-warning btn-sm" onclick="toggleStatus(${dev.deviceID}, ${
          dev.isActive
        })">Toggle</button>
            <a href="editiot.html?id=${dev.deviceID}" class="btn btn-info btn-sm">Edit</a>
          </td>
        `;
        tableBody.appendChild(row);
      });
    } catch (err) {
      tableBody.innerHTML = `<tr><td colspan="9" class="text-danger text-center">Error loading devices</td></tr>`;
    }
  }

  window.confirmDelete = (deviceId) => {
    if (confirm('Are you sure you want to delete this device?')) {
      deleteDevice(deviceId);
    }
  };

  window.deleteDevice = async (deviceId) => {
    try {
      const res = await fetch(`/api/iotdevice/${deviceId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.ok) {
        loadDevices();
      }
    } catch {}
  };

  window.toggleStatus = async (deviceId, currentStatus) => {
    const newStatus = !currentStatus;
    try {
      const res = await fetch(`/api/iotdevice/setstatus/${deviceId}?isActive=${newStatus}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
      });
      console.log(res.url);
      if (res.ok) loadDevices();
    } catch {
      messageBox.innerHTML = '<div class="alert alert-danger">Connection error</div>';
      console.error('Error toggling device status');
    }
  };

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const device = {
      type: document.getElementById('type').value,
      location: document.getElementById('location').value,
      parameters: document.getElementById('parameters').value,
      isActive: document.getElementById('isActive').value === 'true',
      minTemperature: parseFloat(document.getElementById('minTemperature').value),
      maxTemperature: parseFloat(document.getElementById('maxTemperature').value),
      minHumidity: parseFloat(document.getElementById('minHumidity').value),
      maxHumidity: parseFloat(document.getElementById('maxHumidity').value),
    };
    try {
      const res = await fetch('/api/iotdevice', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(device),
      });
      if (res.ok) {
        form.reset();
        messageBox.innerHTML = '<div class="alert alert-success">Device added successfully!</div>';
        loadDevices();
      } else {
        const text = await res.text();
        messageBox.innerHTML = `<div class="alert alert-danger">${text}</div>`;
      }
    } catch {
      messageBox.innerHTML = '<div class="alert alert-danger">Connection error</div>';
    }
  });

  loadDevices();

  document.getElementById('searchInput').addEventListener('input', function () {
    const query = this.value.toLowerCase();
    document.querySelectorAll('tbody tr').forEach((row) => {
      row.style.display = row.innerText.toLowerCase().includes(query) ? '' : 'none';
    });
  });
});
