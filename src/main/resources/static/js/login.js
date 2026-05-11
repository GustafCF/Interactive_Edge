class LoginSystem {
    constructor() {
        this.authBaseUrl = '/auth';
        this.userBaseUrl = '/us';
        this.initEventListeners();
        this.initializePasswordToggle();
        this.initModalEventListeners();
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

    initModalEventListeners() {
        // Modais
        const registerModal = document.getElementById('registerModal');
        const forgotModal = document.getElementById('forgotPasswordModal');
        const resetModal = document.getElementById('resetPasswordModal');
        
        // Links
        const registerLink = document.getElementById('registerLink');
        const forgotLink = document.getElementById('forgotPasswordLink');
        
        // Botões de fechar
        const closeRegister = document.getElementById('closeRegisterModal');
        const closeForgot = document.getElementById('closeForgotModal');
        const closeReset = document.getElementById('closeResetModal');
        
        // Botões de cancelar
        const cancelRegister = document.getElementById('cancelRegister');
        const cancelForgot = document.getElementById('cancelForgot');
        const cancelReset = document.getElementById('cancelReset');
        
        // Forms
        const registerForm = document.getElementById('registerForm');
        const forgotForm = document.getElementById('forgotPasswordForm');
        const resetForm = document.getElementById('resetPasswordForm');
        
        // Abrir modais
        if (registerLink) {
            registerLink.addEventListener('click', () => this.openModal(registerModal));
        }
        
        if (forgotLink) {
            forgotLink.addEventListener('click', () => this.openModal(forgotModal));
        }
        
        // Fechar modais (X)
        if (closeRegister) closeRegister.addEventListener('click', () => this.closeModal(registerModal));
        if (closeForgot) closeForgot.addEventListener('click', () => this.closeModal(forgotModal));
        if (closeReset) closeReset.addEventListener('click', () => this.closeModal(resetModal));
        
        // Fechar modais (Cancelar)
        if (cancelRegister) cancelRegister.addEventListener('click', () => this.closeModal(registerModal));
        if (cancelForgot) cancelForgot.addEventListener('click', () => this.closeModal(forgotModal));
        if (cancelReset) cancelReset.addEventListener('click', () => this.closeModal(resetModal));
        
        // Fechar ao clicar fora do modal
        window.addEventListener('click', (e) => {
            if (e.target === registerModal) this.closeModal(registerModal);
            if (e.target === forgotModal) this.closeModal(forgotModal);
            if (e.target === resetModal) this.closeModal(resetModal);
        });
        
        // Submissão dos forms
        if (registerForm) {
            registerForm.addEventListener('submit', (e) => this.handleRegister(e));
        }
        
        if (forgotForm) {
            forgotForm.addEventListener('submit', (e) => this.handleForgotPassword(e));
        }
        
        if (resetForm) {
            resetForm.addEventListener('submit', (e) => this.handleResetPassword(e));
        }
        
        // Verificar se há token na URL para redefinição de senha
        this.checkForResetToken();
    }
    
    checkForResetToken() {
        const urlParams = new URLSearchParams(window.location.search);
        const token = urlParams.get('token');
        
        if (token) {
            const resetTokenInput = document.getElementById('resetToken');
            if (resetTokenInput) {
                resetTokenInput.value = token;
            }
            
            const resetModal = document.getElementById('resetPasswordModal');
            if (resetModal) {
                this.openModal(resetModal);
                
                const newUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
                window.history.replaceState({}, document.title, newUrl);
            }
        }
    }
    
    openModal(modal) {
        if (modal) {
            modal.style.display = 'block';
            document.body.style.overflow = 'hidden';
        }
    }
    
    closeModal(modal) {
        if (modal) {
            modal.style.display = 'none';
            document.body.style.overflow = 'auto';
            this.clearModalErrors(modal);
            
            const form = modal.querySelector('form');
            if (form) form.reset();
        }
    }
    
    clearModalErrors(modal) {
        const errorMessages = modal.querySelectorAll('.error-message');
        errorMessages.forEach(error => {
            error.textContent = '';
            error.style.display = 'none';
        });
        
        const inputs = modal.querySelectorAll('.form-control');
        inputs.forEach(input => {
            input.classList.remove('error');
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
    
    validateResetForm() {
        let isValid = true;
        const token = document.getElementById('resetToken').value.trim();
        const newPassword = document.getElementById('newPassword').value;
        
        ['resetTokenError', 'newPasswordError'].forEach(id => {
            const errorEl = document.getElementById(id);
            if (errorEl) {
                errorEl.textContent = '';
                errorEl.style.display = 'none';
            }
        });
        
        if (!token) {
            this.showError('resetTokenError', 'Token é obrigatório');
            isValid = false;
        }
        
        if (!newPassword) {
            this.showError('newPasswordError', 'Nova senha é obrigatória');
            isValid = false;
        } else if (newPassword.length < 6) {
            this.showError('newPasswordError', 'Senha deve ter pelo menos 6 caracteres');
            isValid = false;
        }
        
        return isValid;
    }
    
    isValidPhone(phone) {
        const re = /^\([1-9]{2}\) (?:[2-8]|9[0-9])[0-9]{3}-[0-9]{4}$|^[1-9]{2}9[0-9]{8}$/;
        return re.test(phone);
    }

    clearErrors() {
        const errorElements = document.querySelectorAll('.error-message');
        errorElements.forEach(element => {
            element.textContent = '';
            element.style.display = 'none';
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
        
        const input = document.getElementById(elementId.replace('Error', ''));
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

        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value;
        const loginBtn = document.getElementById('loginBtn');
        
        loginBtn.disabled = true;
        loginBtn.classList.add('loading');
        
        try {
            const response = await this.performLogin(email, password);
            
            if (response.ok) {
                const data = await response.json();
                await this.handleLoginSuccess(data, email);
            } else {
                this.handleLoginError(response);
            }
        } catch (error) {
            console.error('Login error:', error);
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
        
        return fetch(`${this.authBaseUrl}/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(loginData)
        });
    }

    async handleLoginSuccess(data, email) {
        console.log('=== Login Success ===');
        
        if (data.accessToken) {
            // Salvar token
            localStorage.setItem('jwtToken', data.accessToken);
            
            // Salvar expiração do token
            const tokenExpiry = Date.now() + (data.expiresIn * 1000);
            localStorage.setItem('tokenExpiry', tokenExpiry);
            
            // CORREÇÃO: Salvar informações do usuário
            localStorage.setItem('userInfo', email);
            localStorage.setItem('userEmail', email);
            
            console.log('Email salvo no localStorage:', email);
            console.log('Token salvo:', data.accessToken.substring(0, 20) + '...');
            
            // Buscar nome do usuário do backend
            await this.fetchUserName(email, data.accessToken);
            
            this.showAlert('Login realizado com sucesso! Redirecionando...', 'success');
            
            setTimeout(() => {
                window.location.href = '/';
            }, 1500);
        } else {
            console.error('Token não recebido do backend');
            this.showAlert('Erro ao realizar login. Token não recebido.', 'error');
        }
    }
    
    async fetchUserName(email, token) {
        try {
            console.log('Buscando nome do usuário para:', email);
            
            const response = await fetch(`${this.userBaseUrl}/fd/em/${email}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            
            if (response.ok) {
                const userData = await response.json();
                console.log('Dados do usuário recebidos:', userData);
                
                if (userData.name) {
                    localStorage.setItem('userName', userData.name);
                    console.log('Nome do usuário salvo:', userData.name);
                } else {
                    // Fallback: usar parte do email como nome
                    const nameFromEmail = email.split('@')[0];
                    localStorage.setItem('userName', nameFromEmail);
                    console.log('Nome fallback salvo:', nameFromEmail);
                }
            } else {
                console.error('Erro ao buscar usuário:', response.status);
                // Fallback: usar parte do email como nome
                const nameFromEmail = email.split('@')[0];
                localStorage.setItem('userName', nameFromEmail);
                console.log('Nome fallback (erro) salvo:', nameFromEmail);
            }
        } catch (error) {
            console.error('Erro ao buscar nome do usuário:', error);
            // Fallback: usar parte do email como nome
            const nameFromEmail = email.split('@')[0];
            localStorage.setItem('userName', nameFromEmail);
            console.log('Nome fallback (exceção) salvo:', nameFromEmail);
        }
    }

    handleLoginError(response, errorMessage = null) {
        if (response) {
            if (response.status === 401) {
                this.showAlert('E-mail ou senha incorretos.', 'error');
            } else if (response.status === 403) {
                this.showAlert('Acesso negado. Sua conta pode estar bloqueada.', 'error');
            } else if (response.status === 404) {
                this.showAlert('Usuário não encontrado.', 'error');
            } else if (response.status === 429) {
                this.showAlert('Muitas tentativas. Tente novamente em alguns minutos.', 'error');
            } else if (response.status === 500) {
                this.showAlert('Erro no servidor. Tente novamente mais tarde.', 'error');
            } else {
                this.showAlert('Erro ao realizar login. Tente novamente.', 'error');
            }
        } else {
            this.showAlert(errorMessage || 'Erro de conexão. Verifique sua internet.', 'error');
        }
    }
    
    async handleRegister(event) {
        event.preventDefault();
        
        const name = document.getElementById('regName').value.trim();
        const email = document.getElementById('regEmail').value.trim();
        const password = document.getElementById('regPassword').value;
        const phone = document.getElementById('regPhone').value.trim();
        
        const submitBtn = document.getElementById('registerSubmitBtn');
        const originalText = submitBtn.innerHTML;
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Registrando...';
        
        try {
            const createUserDto = {
                name: name,
                email: email,
                password: password,
                phone: phone || null
            };
            
            const response = await fetch(`${this.userBaseUrl}/register`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(createUserDto)
            });
            
            if (response.ok) {
                const userData = await response.json();
                this.showAlert('Registro realizado com sucesso! Faça login.', 'success');
                this.closeModal(document.getElementById('registerModal'));
                
                document.getElementById('email').value = email;
                
                document.getElementById('regName').value = '';
                document.getElementById('regEmail').value = '';
                document.getElementById('regPassword').value = '';
                document.getElementById('regPhone').value = '';
            } else {
                let errorMessage = 'Erro ao realizar registro.';
                try {
                    const error = await response.json();
                    errorMessage = error.message || errorMessage;
                    
                    if (response.status === 409 || errorMessage.toLowerCase().includes('email') || errorMessage.toLowerCase().includes('duplicate')) {
                        this.showError('regEmailError', 'E-mail já cadastrado');
                        this.showAlert('Este e-mail já está em uso.', 'error');
                    } else if (response.status === 400) {
                        this.showAlert('Dados inválidos. Verifique os campos e tente novamente.', 'error');
                    } else {
                        this.showAlert(errorMessage, 'error');
                    }
                } catch (e) {
                    console.error('Error parsing error response:', e);
                    if (response.status === 400) {
                        this.showAlert('Dados inválidos. Verifique os campos e tente novamente.', 'error');
                    } else if (response.status === 409) {
                        this.showError('regEmailError', 'E-mail já cadastrado');
                        this.showAlert('Este e-mail já está em uso.', 'error');
                    } else {
                        this.showAlert('Erro ao realizar registro. Tente novamente.', 'error');
                    }
                }
            }
        } catch (error) {
            console.error('Register error:', error);
            this.showAlert('Erro de conexão. Verifique sua internet e tente novamente.', 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        }
    }
    
    async handleForgotPassword(event) {
        event.preventDefault();
    
        const email = document.getElementById('forgotEmail').value.trim();
        const submitBtn = document.getElementById('forgotSubmitBtn');
        const originalText = submitBtn.innerHTML;
        
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Enviando...';
        
        try {
            const response = await fetch(`${this.authBaseUrl}/forgot-password`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email })
            });
            
            if (response.ok) {
                this.showAlert('E-mail de recuperação enviado! Verifique sua caixa de entrada.', 'success');
                this.closeModal(document.getElementById('forgotPasswordModal'));
                document.getElementById('forgotEmail').value = '';
            } else {
                let errorMessage = 'Erro ao enviar e-mail de recuperação.';
                try {
                    const error = await response.json();
                    errorMessage = error.message || errorMessage;
                } catch (e) {
                    if (response.status === 404) {
                        errorMessage = 'Usuário não encontrado.';
                    }
                }
                this.showAlert(errorMessage, 'error');
            }
        } catch (error) {
            console.error('Forgot password error:', error);
            this.showAlert('Erro de conexão. Tente novamente.', 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        }
    }
    
    async handleResetPassword(event) {
        event.preventDefault();
        
        if (!this.validateResetForm()) {
            return;
        }
        
        const token = document.getElementById('resetToken').value.trim();
        const newPassword = document.getElementById('newPassword').value;
        const submitBtn = document.getElementById('resetSubmitBtn');
        const originalText = submitBtn.innerHTML;
        
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Redefinindo...';
        
        try {
            const response = await fetch(`${this.authBaseUrl}/reset-password`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ token, newPassword })
            });
            
            if (response.ok) {
                this.showAlert('Senha redefinida com sucesso! Faça login com sua nova senha.', 'success');
                this.closeModal(document.getElementById('resetPasswordModal'));
                document.getElementById('resetToken').value = '';
                document.getElementById('newPassword').value = '';
            } else {
                let errorMessage = 'Erro ao redefinir senha.';
                try {
                    const error = await response.json();
                    errorMessage = error.message || errorMessage;
                } catch (e) {
                    if (response.status === 400) {
                        errorMessage = 'Token inválido ou expirado. Solicite uma nova recuperação de senha.';
                    } else if (response.status === 404) {
                        errorMessage = 'Token não encontrado.';
                    }
                }
                this.showAlert(errorMessage, 'error');
            }
        } catch (error) {
            console.error('Reset password error:', error);
            this.showAlert('Erro de conexão. Tente novamente.', 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
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
            info: 'info-circle',
            warning: 'exclamation-triangle'
        };
        
        alert.innerHTML = `
            <i class="fas fa-${icons[type] || 'info-circle'}"></i>
            <span>${this.escapeHtml(message)}</span>
            <button class="alert-close" onclick="this.parentElement.remove()">
                <i class="fas fa-times"></i>
            </button>
        `;
        
        alertContainer.appendChild(alert);
        
        const timeout = type === 'success' ? 5000 : 8000;
        setTimeout(() => {
            if (alert.parentNode) {
                alert.style.animation = 'slideOut 0.3s ease';
                setTimeout(() => alert.remove(), 300);
            }
        }, timeout);
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Inicialização
document.addEventListener('DOMContentLoaded', () => {
    window.loginSystem = new LoginSystem();
});

// Função global para mostrar alertas
function showAlert(message, type = 'info') {
    if (window.loginSystem) {
        window.loginSystem.showAlert(message, type);
    }
}