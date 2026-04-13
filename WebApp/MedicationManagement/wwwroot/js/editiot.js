addEventListener('DOMContentLoaded', async () => {
  const form = document.getElementById('iotEditForm');
  const messageBox = document.getElementById('message');

  const logoutBtn = document.getElementById('logoutBtn');
  logoutBtn.addEventListener('click', () => {
    localStorage.removeItem('token');
    window.location.href = 'login.html';
  });

  const deviceId = new URLSearchParams(window.location.search).get('id');

  if (!deviceId) {
    messageBox.innerHTML = `<div class="alert alert-danger">No device ID provided.</div>`;
    return;
  }

  const token = localStorage.getItem('token');
  if (!token) {
    window.location.href = 'login.html';
    return;
  }

  try {
    const response = await fetch(`/api/iotdevice/${deviceId}`, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error('Device not found or unauthorized access.');
    }

    const device = await response.json();
    form.type.value = device.type;
    form.location.value = device.location;
    form.parameters.value = device.parameters;
    form.minTemperature.value = device.minTemperature || '';
    form.maxTemperature.value = device.maxTemperature || '';
    form.minHumidity.value = device.minHumidity || '';
    form.maxHumidity.value = device.maxHumidity || '';
  } catch (error) {
    messageBox.innerHTML = `<div class="alert alert-danger">Error loading device details: ${error.message}</div>`;
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const patchDoc = [
      { op: 'replace', path: '/type', value: document.getElementById('type').value },
      { op: 'replace', path: '/location', value: document.getElementById('location').value },
      { op: 'replace', path: '/parameters', value: document.getElementById('parameters').value },
      {
        op: 'replace',
        path: '/minTemperature',
        value: parseFloat(document.getElementById('minTemperature').value) || null,
      },
      {
        op: 'replace',
        path: '/maxTemperature',
        value: parseFloat(document.getElementById('maxTemperature').value) || null,
      },
      {
        op: 'replace',
        path: '/minHumidity',
        value: parseFloat(document.getElementById('minHumidity').value) || null,
      },
      {
        op: 'replace',
        path: '/maxHumidity',
        value: parseFloat(document.getElementById('maxHumidity').value) || null,
      },
    ];
    try {
      const response = await fetch(`/api/iotdevice/${deviceId}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(patchDoc),
      });

      console.log(patchDoc);

      if (!response.ok) {
        throw new Error('Failed to update device details.');
      }

      messageBox.innerHTML = '<div class="alert alert-success">Device updated successfully!</div>';
    } catch (error) {
      messageBox.innerHTML = `<div class="alert alert-danger">Error updating device: ${error.message}</div>`;
    }
  });
});
