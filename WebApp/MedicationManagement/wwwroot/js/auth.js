document.addEventListener('DOMContentLoaded', () => {
  const loginForm = document.getElementById('loginForm');
  const errorBox = document.getElementById('error');

  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      });
      if (response.ok) {
        const data = await response.json();
        localStorage.setItem('token', data.token);
        window.location.href = 'dashboard.html';
      } else {
        errorBox.innerText = 'Login failed. Please check your credentials.';
        errorBox.style.display = 'block';
      }
    } catch (error) {
      errorBox.innerText = 'An error occurred. Please try again later.';
      errorBox.style.display = 'block';
    }
  });
});
