class LoginSystem {
    constructor() {
        this.baseUrl = '/auth';
        this.initEventListeners();
        this.initializePasswordToggle();
    }

    initEventListeners() {
        const loginForm = document.getElementById('loginForm');
        if (loginForm) {
            loginForm.addEventListener('submit', (e) => this.handleLogin(e));
        }

        const togglePassword = document.getElementById('togglePassword');
        if (togglePassword) {
            togglePassword.addEventListener('click', () => this.togglePasswordVisibility());
        }

        document.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && e.target.closest('#loginForm')) {
                this.handleLogin(e);
            }
        });
    }

    initializePasswordToggle() {
        const passwordInput = document.getElementById('password');
        const toggleButton = document.getElementById('togglePassword');
        
        if (passwordInput && toggleButton) {
            passwordInput.addEventListener('input', () => {
                toggleButton.style.display = passwordInput.value ? 'flex' : 'none';
            });
            
            toggleButton.style.display = passwordInput.value ? 'flex' : 'none';
        }
    }

    togglePasswordVisibility() {
        const passwordInput = document.getElementById('password');
        const toggleIcon = document.getElementById('togglePassword').querySelector('i');
        
        if (passwordInput.type === 'password') {
            passwordInput.type = 'text';
            toggleIcon.classList.remove('fa-eye');
            toggleIcon.classList.add('fa-eye-slash');
        } else {
            passwordInput.type = 'password';
            toggleIcon.classList.remove('fa-eye-slash');
            toggleIcon.classList.add('fa-eye');
        }
    }

    validateForm() {
        let isValid = true;
        this.clearErrors();

        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value.trim();

        if (!email) {
            this.showError('emailError', 'E-mail é obrigatório');
            isValid = false;
        } else if (!this.isValidEmail(email)) {
            this.showError('emailError', 'Digite um e-mail válido');
            isValid = false;
        }

        if (!password) {
            this.showError('passwordError', 'Senha é obrigatória');
            isValid = false;
        }

        return isValid;
    }

    clearErrors() {
        const errorElements = document.querySelectorAll('.error-message');
        errorElements.forEach(element => {
            element.textContent = '';
        });
        
        const inputs = document.querySelectorAll('.form-control');
        inputs.forEach(input => {
            input.classList.remove('error');
        });
    }

    showError(elementId, message) {
        const element = document.getElementById(elementId);
        if (element) {
            element.textContent = message;
            element.style.display = 'block';
        }
        
        const inputId = elementId.replace('Error', '');
        const input = document.getElementById(inputId);
        if (input) {
            input.classList.add('error');
        }
    }

    isValidEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    }

    async handleLogin(event) {
        event.preventDefault();
        
        if (!this.validateForm()) {
            return;
        }

        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value;
        const loginBtn = document.getElementById('loginBtn');
        
        loginBtn.disabled = true;
        loginBtn.classList.add('loading');
        
        try {
            const response = await this.performLogin(email, password);
            
            if (response.ok) {
                const data = await response.json();
                this.handleLoginSuccess(data, email);
            } else {
                this.handleLoginError(response);
            }
        } catch (error) {
            this.handleLoginError(null, error.message);
        } finally {
            loginBtn.disabled = false;
            loginBtn.classList.remove('loading');
        }
    }

    async performLogin(email, password) {
        const loginData = {
            email: email,
            password: password
        };
        
        return fetch(`${this.baseUrl}/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(loginData)
        });
    }

    handleLoginSuccess(data, email) {
        this.showAlert('Login realizado com sucesso! Redirecionando...', 'success');
        
        if (data.accessToken) {
            localStorage.setItem('jwtToken', data.accessToken);
            
            const tokenExpiry = Date.now() + (data.expiresIn * 1000);
            localStorage.setItem('tokenExpiry', tokenExpiry);
            localStorage.setItem('userInfo', email);
            
            if (data.user) {
                localStorage.setItem('userRole', data.user.role || 'USER');
                localStorage.setItem('userName', data.user.name || email);
            }
        }
        
        setTimeout(() => {
            window.location.href = '/'; 
        }, 1500);
    }

    handleLoginError(response, errorMessage = null) {
        if (response) {
            switch (response.status) {
                case 401:
                    this.showAlert('E-mail ou senha incorretos.', 'error');
                    break;
                case 403:
                    this.showAlert('Acesso negado. Sua conta pode estar bloqueada.', 'error');
                    break;
                case 404:
                    this.showAlert('Usuário não encontrado.', 'error');
                    break;
                case 429:
                    this.showAlert('Muitas tentativas. Tente novamente em alguns minutos.', 'error');
                    break;
                case 500:
                    this.showAlert('Erro no servidor. Tente novamente mais tarde.', 'error');
                    break;
                default:
                    this.showAlert('Erro ao realizar login. Tente novamente.', 'error');
            }
        } else {
            this.showAlert(errorMessage || 'Erro de conexão. Verifique sua internet.', 'error');
        }
    }

    showAlert(message, type = 'info') {
        const alertContainer = document.getElementById('alertContainer');
        if (!alertContainer) return;
        
        const existingAlerts = alertContainer.querySelectorAll('.alert');
        existingAlerts.forEach(alert => {
            alert.style.animation = 'slideOut 0.3s ease';
            setTimeout(() => alert.remove(), 300);
        });
        
        const alert = document.createElement('div');
        alert.className = `alert alert-${type}`;
        
        const icons = {
            success: 'check-circle',
            error: 'exclamation-circle',
            info: 'info-circle'
        };
        
        alert.innerHTML = `
            <i class="fas fa-${icons[type] || 'info-circle'}"></i>
            <span>${message}</span>
            <button class="alert-close" onclick="this.parentElement.remove()">
                <i class="fas fa-times"></i>
            </button>
        `;
        
        alertContainer.appendChild(alert);
        
        if (type === 'success') {
            setTimeout(() => {
                if (alert.parentNode) {
                    alert.style.animation = 'slideOut 0.3s ease';
                    setTimeout(() => alert.remove(), 300);
                }
            }, 5000);
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.loginSystem = new LoginSystem();
});

function showAlert(message, type = 'info') {
    if (window.loginSystem) {
        window.loginSystem.showAlert(message, type);
    }
}