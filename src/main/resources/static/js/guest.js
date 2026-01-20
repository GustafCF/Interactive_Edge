// guest.js - JavaScript para gerenciamento de hóspedes

class GuestManager {
    constructor() {
        this.baseUrl = '';
        this.token = localStorage.getItem('jwtToken');
        this.editingGuestId = null;
        this.initEventListeners();
        this.checkAuthentication();
        this.loadGuests();
    }

    checkAuthentication() {
        if (!this.token) {
            window.location.href = '/login';
            return;
        }
        
        const tokenExpiry = localStorage.getItem('tokenExpiry');
        if (tokenExpiry && Date.now() > parseInt(tokenExpiry)) {
            this.logout();
            return;
        }
        
        this.loadUserInfo();
    }

    async loadUserInfo() {
        try {
            const userInfoElement = document.getElementById('userInfo');
            const userEmail = localStorage.getItem('userInfo');
            
            if (userInfoElement && userEmail) {
                userInfoElement.innerHTML = `
                    <i class="fas fa-user"></i> ${userEmail}
                `;
            } else if (userInfoElement) {
                userInfoElement.innerHTML = '<i class="fas fa-user"></i> Usuário';
            }
        } catch (error) {
            console.error('Erro ao carregar informações do usuário:', error);
        }
    }

    initEventListeners() {
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.logout();
            });
        }

        const saveBtn = document.getElementById('saveGuestBtn');
        if (saveBtn) {
            saveBtn.addEventListener('click', () => this.saveGuest());
        }

        const cancelBtn = document.getElementById('cancelEditBtn');
        if (cancelBtn) {
            cancelBtn.addEventListener('click', () => this.cancelEdit());
        }

        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => this.searchGuests(e.target.value));
        }

        const refreshBtn = document.getElementById('refreshGuestsBtn');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => this.loadGuests());
        }

        const closeButtons = document.querySelectorAll('.close');
        closeButtons.forEach(button => {
            button.addEventListener('click', () => this.closeModal(button.closest('.modal')));
        });

        const cancelDeleteBtn = document.getElementById('cancelDeleteBtn');
        if (cancelDeleteBtn) {
            cancelDeleteBtn.addEventListener('click', () => this.closeModal(document.getElementById('confirmModal')));
        }

        document.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.saveGuest();
            }
        });

        window.addEventListener('click', (e) => {
            const modals = document.querySelectorAll('.modal');
            modals.forEach(modal => {
                if (e.target === modal) {
                    this.closeModal(modal);
                }
            });
        });
    }

    logout() {
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('tokenExpiry');
        localStorage.removeItem('userInfo');
        window.location.href = '/login';
    }

    async makeAuthenticatedRequest(url, options = {}) {
        if (!this.token) {
            this.logout();
            throw new Error('Token de autenticação não encontrado');
        }

        const defaultOptions = {
            headers: {
                'Authorization': `Bearer ${this.token}`,
                'Content-Type': 'application/json',
                ...options.headers
            }
        };

        const mergedOptions = { ...defaultOptions, ...options };
        
        try {
            const response = await fetch(url, mergedOptions);
            
            if (response.status === 401 || response.status === 403) {
                this.logout();
                throw new Error('Sessão expirada. Faça login novamente.');
            }
            
            return response;
        } catch (error) {
            console.error('Erro na requisição:', error);
            throw error;
        }
    }

    async loadGuests() {
        try {
            const guestList = document.getElementById('guestList');
            guestList.innerHTML = '<p class="loading-text">Carregando hóspedes...</p>';
            
            const response = await this.makeAuthenticatedRequest('/guest/all');
            
            if (response.ok) {
                const guests = await response.json();
                this.displayGuests(guests);
            } else {
                throw new Error('Erro ao carregar hóspedes');
            }
        } catch (error) {
            console.error('Erro ao carregar hóspedes:', error);
            this.showAlert('Erro ao carregar lista de hóspedes', 'error');
            document.getElementById('guestList').innerHTML = '<p class="error-text">Erro ao carregar hóspedes</p>';
        }
    }

    displayGuests(guests) {
        const container = document.getElementById('guestList');
        if (!container) return;

        if (guests.length === 0) {
            container.innerHTML = '<p class="no-data">Nenhum hóspede cadastrado</p>';
            return;
        }

        const html = `
            <table class="table">
                <thead>
                    <tr>
                        <th><i class="fas fa-user"></i> Nome</th>
                        <th><i class="fas fa-id-card"></i> RG</th>
                        <th><i class="fas fa-phone"></i> Telefone</th>
                        <th><i class="fas fa-envelope"></i> Email</th>
                        <th><i class="fas fa-calendar-check"></i> Reservas</th>
                        <th><i class="fas fa-cog"></i> Ações</th>
                    </tr>
                </thead>
                <tbody>
                    ${guests.map(guest => `
                        <tr>
                            <td>${this.escapeHtml(guest.name)}</td>
                            <td>${this.escapeHtml(guest.rg)}</td>
                            <td>${this.escapeHtml(guest.phone)}</td>
                            <td>${this.escapeHtml(guest.email || 'N/A')}</td>
                            <td>
                                <span class="badge ${guest.reservation && guest.reservation.length > 0 ? 'badge-busy' : 'badge-vague'}">
                                    ${guest.reservation ? guest.reservation.length : 0} reserva(s)
                                </span>
                            </td>
                            <td>
                                <div class="action-buttons">
                                    <button class="btn-action btn-edit" data-id="${guest.id}" data-name="${this.escapeAttr(guest.name)}" data-rg="${this.escapeAttr(guest.rg)}" data-phone="${this.escapeAttr(guest.phone)}" data-email="${this.escapeAttr(guest.email || '')}">
                                        <i class="fas fa-edit"></i>
                                    </button>
                                    <button class="btn-action btn-delete" data-id="${guest.id}" data-name="${this.escapeAttr(guest.name)}">
                                        <i class="fas fa-trash"></i>
                                    </button>
                                </div>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;

        container.innerHTML = html;
        
        // Adicionar event listeners aos botões de ação
        this.addActionButtonListeners();
    }

    addActionButtonListeners() {
        // Botões de editar
        document.querySelectorAll('.btn-edit').forEach(button => {
            button.addEventListener('click', (e) => {
                const id = e.currentTarget.dataset.id;
                const name = e.currentTarget.dataset.name;
                const rg = e.currentTarget.dataset.rg;
                const phone = e.currentTarget.dataset.phone;
                const email = e.currentTarget.dataset.email;
                this.editGuest(id, name, rg, phone, email);
            });
        });

        // Botões de excluir
        document.querySelectorAll('.btn-delete').forEach(button => {
            button.addEventListener('click', (e) => {
                const id = e.currentTarget.dataset.id;
                const name = e.currentTarget.dataset.name;
                this.showDeleteConfirmation(id, name);
            });
        });
    }

    async searchGuests(searchTerm) {
        const resultsContainer = document.getElementById('searchResults');
        if (!resultsContainer) return;

        if (searchTerm.length < 2) {
            resultsContainer.innerHTML = '';
            return;
        }

        try {
            const response = await this.makeAuthenticatedRequest('/guest/all');
            
            if (response.ok) {
                const allGuests = await response.json();
                const filteredGuests = allGuests.filter(guest => 
                    guest.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                    guest.rg.toLowerCase().includes(searchTerm.toLowerCase()) ||
                    guest.phone.toLowerCase().includes(searchTerm.toLowerCase())
                );

                if (filteredGuests.length === 0) {
                    resultsContainer.innerHTML = '<p class="no-results">Nenhum hóspede encontrado</p>';
                    return;
                }

                const html = filteredGuests.map(guest => `
                    <div class="search-result-item">
                        <strong>${this.escapeHtml(guest.name)}</strong><br>
                        <strong>RG:</strong> ${this.escapeHtml(guest.rg)}<br>
                        <strong>Telefone:</strong> ${this.escapeHtml(guest.phone)}<br>
                        <strong>Email:</strong> ${this.escapeHtml(guest.email || 'N/A')}<br>
                        <strong>Reservas:</strong> <span class="badge ${guest.reservation && guest.reservation.length > 0 ? 'badge-busy' : 'badge-vague'}">
                            ${guest.reservation ? guest.reservation.length : 0}
                        </span>
                        <div style="margin-top: 8px;">
                            <button class="btn btn-sm btn-primary edit-search-btn" data-id="${guest.id}" data-name="${this.escapeAttr(guest.name)}" data-rg="${this.escapeAttr(guest.rg)}" data-phone="${this.escapeAttr(guest.phone)}" data-email="${this.escapeAttr(guest.email || '')}">
                                <i class="fas fa-edit"></i> Editar
                            </button>
                        </div>
                    </div>
                `).join('');

                resultsContainer.innerHTML = html;
                
                // Adicionar event listeners aos botões de editar nos resultados
                document.querySelectorAll('.edit-search-btn').forEach(button => {
                    button.addEventListener('click', (e) => {
                        const id = e.currentTarget.dataset.id;
                        const name = e.currentTarget.dataset.name;
                        const rg = e.currentTarget.dataset.rg;
                        const phone = e.currentTarget.dataset.phone;
                        const email = e.currentTarget.dataset.email;
                        this.editGuest(id, name, rg, phone, email);
                        
                        // Limpar resultados da busca
                        resultsContainer.innerHTML = '';
                        document.getElementById('searchInput').value = '';
                    });
                });
            }
        } catch (error) {
            console.error('Erro na busca:', error);
            resultsContainer.innerHTML = '<p class="error-text">Erro na busca</p>';
        }
    }

    validateGuestForm() {
        let isValid = true;
        
        // Limpar erros anteriores
        this.clearErrors();
        
        const name = document.getElementById('guestName').value.trim();
        const rg = document.getElementById('guestRg').value.trim();
        const phone = document.getElementById('guestPhone').value.trim();
        const email = document.getElementById('guestEmail').value.trim();

        // Validar nome
        if (!name) {
            this.showError('nameError', 'Nome é obrigatório');
            isValid = false;
        }

        // Validar RG
        if (!rg) {
            this.showError('rgError', 'RG é obrigatório');
            isValid = false;
        }

        // Validar telefone
        if (!phone) {
            this.showError('phoneError', 'Telefone é obrigatório');
            isValid = false;
        } else if (!this.isValidPhone(phone)) {
            this.showError('phoneError', 'Telefone inválido');
            isValid = false;
        }

        // Validar email (opcional)
        if (email && !this.isValidEmail(email)) {
            this.showError('emailError', 'Email inválido');
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
        }
        
        // Adicionar classe de erro ao input correspondente
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

    isValidPhone(phone) {
        // Aceita diversos formatos de telefone
        const re = /^(\+\d{1,3})?\s?\(?\d{1,4}\)?[\s.-]?\d{1,4}[\s.-]?\d{1,9}$/;
        return re.test(phone);
    }

    async saveGuest() {
        if (!this.validateGuestForm()) {
            return;
        }

        const name = document.getElementById('guestName').value.trim();
        const rg = document.getElementById('guestRg').value.trim();
        const phone = document.getElementById('guestPhone').value.trim();
        const email = document.getElementById('guestEmail').value.trim();

        const guestData = {
            name: name,
            rg: rg,
            phone: phone,
            email: email
        };

        try {
            let response;
            if (this.editingGuestId) {
                // Atualizar hóspede existente
                response = await this.makeAuthenticatedRequest(`/guest/update/${this.editingGuestId}`, {
                    method: 'PUT',
                    body: JSON.stringify(guestData)
                });
            } else {
                // Criar novo hóspede
                response = await this.makeAuthenticatedRequest('/guest/insert', {
                    method: 'POST',
                    body: JSON.stringify(guestData)
                });
            }

            if (response.ok) {
                const savedGuest = await response.json();
                this.showAlert(
                    this.editingGuestId ? 'Hóspede atualizado com sucesso!' : 'Hóspede cadastrado com sucesso!', 
                    'success'
                );
                this.clearForm();
                this.loadGuests();
            } else {
                throw new Error('Erro ao salvar hóspede');
            }
        } catch (error) {
            console.error('Erro ao salvar hóspede:', error);
            this.showAlert('Erro ao salvar hóspede', 'error');
        }
    }

    editGuest(id, name, rg, phone, email) {
        this.editingGuestId = id;
        
        document.getElementById('guestName').value = this.unescapeHtml(name);
        document.getElementById('guestRg').value = this.unescapeHtml(rg);
        document.getElementById('guestPhone').value = this.unescapeHtml(phone);
        document.getElementById('guestEmail').value = this.unescapeHtml(email);
        
        document.getElementById('formTitle').textContent = 'Editar Hóspede';
        document.getElementById('saveGuestBtn').innerHTML = '<i class="fas fa-save"></i> Atualizar Hóspede';
        document.getElementById('cancelEditBtn').style.display = 'inline-flex';
        
        document.getElementById('guestName').scrollIntoView({ behavior: 'smooth' });
    }

    cancelEdit() {
        this.editingGuestId = null;
        this.clearForm();
    }

    clearForm() {
        document.getElementById('guestName').value = '';
        document.getElementById('guestRg').value = '';
        document.getElementById('guestPhone').value = '';
        document.getElementById('guestEmail').value = '';
        
        document.getElementById('formTitle').textContent = 'Cadastrar Novo Hóspede';
        document.getElementById('saveGuestBtn').innerHTML = '<i class="fas fa-save"></i> Salvar Hóspede';
        document.getElementById('cancelEditBtn').style.display = 'none';
        
        document.getElementById('searchInput').value = '';
        document.getElementById('searchResults').innerHTML = '';
        
        this.clearErrors();
    }

    showDeleteConfirmation(id, name) {
        this.deleteGuestId = id;
        document.getElementById('deleteGuestName').textContent = this.unescapeHtml(name);
        this.openModal('confirmModal');
    }

    async deleteGuest() {
        if (!this.deleteGuestId) return;

        try {
            const response = await this.makeAuthenticatedRequest(`/guest/delete/${this.deleteGuestId}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                this.showAlert('Hóspede excluído com sucesso!', 'success');
                this.closeModal(document.getElementById('confirmModal'));
                this.loadGuests();
                this.deleteGuestId = null;
            } else {
                throw new Error('Erro ao excluir hóspede');
            }
        } catch (error) {
            console.error('Erro ao excluir hóspede:', error);
            this.showAlert('Erro ao excluir hóspede', 'error');
            this.closeModal(document.getElementById('confirmModal'));
        }
    }

    openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'block';
            
            if (modalId === 'confirmModal') {
                const confirmBtn = document.getElementById('confirmDeleteBtn');
                confirmBtn.onclick = () => this.deleteGuest();
            }
        }
    }

    closeModal(modal) {
        if (modal) {
            modal.style.display = 'none';
        }
    }

    showAlert(message, type = 'info') {
        const existingAlerts = document.querySelectorAll('.alert');
        existingAlerts.forEach(alert => alert.remove());

        const alert = document.createElement('div');
        alert.className = `alert alert-${type}`;
        alert.innerHTML = `
            <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'}"></i>
            ${message}
        `;

        document.body.appendChild(alert);

        setTimeout(() => {
            alert.style.animation = 'slideOut 0.3s ease';
            setTimeout(() => {
                if (alert.parentNode) {
                    alert.parentNode.removeChild(alert);
                }
            }, 300);
        }, 5000);
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    escapeAttr(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML.replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    unescapeHtml(text) {
        const div = document.createElement('div');
        div.innerHTML = text;
        return div.textContent;
    }
}

// Inicializar quando o DOM estiver carregado
document.addEventListener('DOMContentLoaded', () => {
    window.guestManager = new GuestManager();
});