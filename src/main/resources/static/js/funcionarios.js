// funcionarios.js - Versão corrigida com melhor tratamento do role update
class FuncionarioManager {
    constructor() {
        this.baseUrl = '/us';
        // this.baseUrl = 'http://192.168.1.100:8080/us';
        this.token = localStorage.getItem('jwtToken');
        this.currentPage = 1;
        this.itemsPerPage = 10;
        this.totalPages = 1;
        this.funcionarios = [];
        this.currentEditId = null;
        
        console.log('FuncionarioManager inicializado');
        console.log('Token:', this.token ? 'Presente' : 'Ausente');
        
        if (this.token) {
            this.initEventListeners();
            this.loadFuncionarios();
        } else {
            console.error('Token não encontrado, redirecionando...');
            window.location.href = '/login';
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

        const novoFuncBtn = document.getElementById('novoFuncionarioBtn');
        if (novoFuncBtn) {
            novoFuncBtn.addEventListener('click', () => {
                this.openModal();
            });
        }

        const searchBtn = document.getElementById('searchBtn');
        if (searchBtn) {
            searchBtn.addEventListener('click', () => {
                this.searchFuncionarios();
            });
        }

        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('keyup', (e) => {
                if (e.key === 'Enter') {
                    this.searchFuncionarios();
                }
            });
        }

        const filterStatus = document.getElementById('filterStatus');
        if (filterStatus) {
            filterStatus.addEventListener('change', () => {
                this.currentPage = 1;
                this.renderTable();
            });
        }

        const prevPage = document.getElementById('prevPage');
        if (prevPage) {
            prevPage.addEventListener('click', () => {
                this.changePage(-1);
            });
        }

        const nextPage = document.getElementById('nextPage');
        if (nextPage) {
            nextPage.addEventListener('click', () => {
                this.changePage(1);
            });
        }

        const modal = document.getElementById('funcionarioModal');
        const confirmModal = document.getElementById('confirmModal');
        const closeBtns = document.querySelectorAll('.close');
        const cancelBtn = document.getElementById('cancelBtn');
        const cancelDeleteBtn = document.getElementById('cancelDeleteBtn');

        if (closeBtns) {
            closeBtns.forEach(btn => {
                btn.addEventListener('click', () => {
                    if (modal) modal.style.display = 'none';
                    if (confirmModal) confirmModal.style.display = 'none';
                });
            });
        }

        if (cancelBtn) {
            cancelBtn.addEventListener('click', () => {
                if (modal) modal.style.display = 'none';
            });
        }

        if (cancelDeleteBtn) {
            cancelDeleteBtn.addEventListener('click', () => {
                if (confirmModal) confirmModal.style.display = 'none';
            });
        }

        const funcionarioForm = document.getElementById('funcionarioForm');
        if (funcionarioForm) {
            funcionarioForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.saveFuncionario();
            });
        }

        const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');
        if (confirmDeleteBtn) {
            confirmDeleteBtn.addEventListener('click', () => {
                this.deleteFuncionario();
            });
        }

        window.addEventListener('click', (e) => {
            if (modal && e.target === modal) {
                modal.style.display = 'none';
            }
            if (confirmModal && e.target === confirmModal) {
                confirmModal.style.display = 'none';
            }
        });
        
        // Role change listener
        const roleSelect = document.getElementById('role');
        if (roleSelect) {
            roleSelect.addEventListener('change', () => {
                this.updateRoleBadge(roleSelect.value);
            });
        }
    }
    
    updateRoleBadge(roleValue) {
        const statusBadge = document.getElementById('statusBadge');
        if (statusBadge) {
            const roleText = this.getRoleText(roleValue);
            const roleClass = this.getRoleBadgeClass(roleValue);
            statusBadge.textContent = roleText;
            statusBadge.className = `badge ${roleClass}`;
        }
    }

    logout() {
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('tokenExpiry');
        localStorage.removeItem('userInfo');
        localStorage.removeItem('userEmail');
        window.location.href = '/login';
    }

    async makeAuthenticatedRequest(url, options = {}) {
        if (!this.token) {
            console.error('Token não encontrado');
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
            console.log('Fazendo requisição para:', url);
            console.log('Método:', mergedOptions.method || 'GET');
            if (mergedOptions.body && typeof mergedOptions.body === 'string') {
                console.log('Body:', mergedOptions.body);
            }
            
            const response = await fetch(url, mergedOptions);
            
            if (response.status === 401 || response.status === 403) {
                console.log('Token inválido ou expirado');
                this.logout();
                throw new Error('Sessão expirada. Faça login novamente.');
            }
            
            if (!response.ok) {
                const errorText = await response.text();
                console.error('Erro na resposta:', response.status, errorText);
                throw new Error(`Erro ${response.status}: ${errorText || 'Erro desconhecido'}`);
            }
            
            return response;
        } catch (error) {
            console.error('Erro na requisição:', error);
            throw error;
        }
    }

    async loadFuncionarios() {
        try {
            console.log('Carregando funcionários...');
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/all`);
            
            if (response.ok) {
                this.funcionarios = await response.json();
                console.log(`Funcionários carregados: ${this.funcionarios.length}`);
                console.log('Primeiro funcionário:', this.funcionarios[0]);
                this.updateStats();
                this.renderTable();
            } else {
                throw new Error('Erro ao carregar funcionários');
            }
        } catch (error) {
            console.error('Erro ao carregar funcionários:', error);
            this.showAlert('Erro ao carregar funcionários: ' + error.message, 'error');
            
            const tbody = document.getElementById('funcionariosBody');
            if (tbody) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="6" class="no-data">
                            <i class="fas fa-exclamation-triangle"></i>
                            Erro ao carregar dados. Tente novamente.
                        </td>
                    </tr>
                `;
            }
        }
    }

    updateStats() {
        const total = this.funcionarios.length;
        // Get roles from the roles Set
        const basic = this.funcionarios.filter(f => 
            f.roles && Array.from(f.roles).some(role => role.roleStatus === 'BASIC')
        ).length;
        const admin = this.funcionarios.filter(f => 
            f.roles && Array.from(f.roles).some(role => role.roleStatus === 'ADMIN')
        ).length;

        console.log('Estatísticas:', { total, basic, admin });

        const totalFuncElement = document.getElementById('totalFuncionarios');
        const totalUsersElement = document.getElementById('totalUsers');
        const basicUsersElement = document.getElementById('basicUsers');
        const adminUsersElement = document.getElementById('adminUsers');

        if (totalFuncElement) totalFuncElement.textContent = `${total} funcionário${total !== 1 ? 's' : ''}`;
        if (totalUsersElement) totalUsersElement.textContent = total;
        if (basicUsersElement) basicUsersElement.textContent = basic;
        if (adminUsersElement) adminUsersElement.textContent = admin;
    }

    renderTable() {
        const tbody = document.getElementById('funcionariosBody');
        if (!tbody) {
            console.error('Elemento tbody não encontrado');
            return;
        }

        const filter = document.getElementById('filterStatus')?.value || 'all';
        const searchTerm = document.getElementById('searchInput')?.value.toLowerCase() || '';

        console.log('Filtro:', filter, 'Busca:', searchTerm);

        let filteredFuncionarios = this.funcionarios;

        if (filter !== 'all') {
            filteredFuncionarios = filteredFuncionarios.filter(funcionario => {
                return funcionario.roles && Array.from(funcionario.roles).some(role => 
                    role.roleStatus === filter
                );
            });
        }

        if (searchTerm) {
            filteredFuncionarios = filteredFuncionarios.filter(funcionario => {
                const name = funcionario.name ? funcionario.name.toLowerCase() : '';
                const email = funcionario.email ? funcionario.email.toLowerCase() : '';
                const phone = funcionario.phone ? funcionario.phone.toLowerCase() : '';
                
                return name.includes(searchTerm) ||
                       email.includes(searchTerm) ||
                       phone.includes(searchTerm);
            });
        }

        console.log(`Funcionários filtrados: ${filteredFuncionarios.length}`);

        const totalItems = filteredFuncionarios.length;
        this.totalPages = Math.ceil(totalItems / this.itemsPerPage);
        const startIndex = (this.currentPage - 1) * this.itemsPerPage;
        const endIndex = startIndex + this.itemsPerPage;
        const paginatedFuncionarios = filteredFuncionarios.slice(startIndex, endIndex);

        this.updatePaginationControls(totalItems);

        if (paginatedFuncionarios.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="no-data">
                        <i class="fas fa-users-slash"></i>
                        ${searchTerm || filter !== 'all' ? 'Nenhum funcionário encontrado com os filtros aplicados' : 'Nenhum funcionário cadastrado'}
                    </td>
                </tr>
            `;
            return;
        }

        const html = paginatedFuncionarios.map(funcionario => {
            const roles = funcionario.roles ? Array.from(funcionario.roles) : [];
            const mainRole = roles.length > 0 ? roles[0] : { roleStatus: 'BASIC' };
            const roleClass = this.getRoleBadgeClass(mainRole.roleStatus);
            const roleText = this.getRoleText(mainRole.roleStatus);
            const escapedName = funcionario.name ? funcionario.name.replace(/'/g, "\\'") : '';
            
            return `
                <tr>
                    <td>${funcionario.id || '-'}</td>
                    <td>${funcionario.name || '-'}</td>
                    <td>${funcionario.email || '-'}</td>
                    <td>${funcionario.phone || '-'}</td>
                    <td>
                        <span class="badge ${roleClass}">
                            ${roleText}
                        </span>
                    </td>
                    <td>
                        <div class="action-buttons">
                            <button class="btn-action btn-edit" 
                                    onclick="funcionarioManager.openModal(${funcionario.id})"
                                    title="Editar">
                                <i class="fas fa-edit"></i>
                            </button>
                            <button class="btn-action btn-delete" 
                                    onclick="funcionarioManager.openDeleteModal(${funcionario.id}, '${escapedName}')"
                                    title="Excluir">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        tbody.innerHTML = html;
    }

    getRoleBadgeClass(roleStatus) {
        switch(roleStatus) {
            case 'ADMIN': return 'badge-busy';
            case 'BASIC': 
            default: return 'badge-vague';
        }
    }

    getRoleText(roleStatus) {
        switch(roleStatus) {
            case 'ADMIN': return 'Administrador';
            case 'BASIC': 
            default: return 'Funcionário';
        }
    }

    updatePaginationControls(totalItems) {
        const prevBtn = document.getElementById('prevPage');
        const nextBtn = document.getElementById('nextPage');
        const pageInfo = document.getElementById('pageInfo');

        if (prevBtn) prevBtn.disabled = this.currentPage <= 1;
        if (nextBtn) nextBtn.disabled = this.currentPage >= this.totalPages;
        if (pageInfo) pageInfo.textContent = `Página ${this.currentPage} de ${this.totalPages || 1} (${totalItems} itens)`;
    }

    changePage(direction) {
        const newPage = this.currentPage + direction;
        
        if (newPage >= 1 && newPage <= this.totalPages) {
            this.currentPage = newPage;
            this.renderTable();
        }
    }

    searchFuncionarios() {
        this.currentPage = 1;
        this.renderTable();
    }

    async openModal(id = null) {
        this.currentEditId = id;
        const modal = document.getElementById('funcionarioModal');
        const form = document.getElementById('funcionarioForm');
        const title = document.getElementById('modalTitle');
        
        if (!modal || !form || !title) {
            console.error('Elementos do modal não encontrados');
            return;
        }
        
        form.reset();
        this.clearErrors();
        
        // Reset role badge
        const roleSelect = document.getElementById('role');
        if (roleSelect) {
            this.updateRoleBadge(roleSelect.value);
        }
        
        if (id) {
            title.textContent = 'Editar Funcionário';
            await this.loadFuncionarioData(id);
            const passwordRequired = document.getElementById('passwordRequired');
            const confirmPasswordRequired = document.getElementById('confirmPasswordRequired');
            const passwordInput = document.getElementById('password');
            const confirmPasswordInput = document.getElementById('confirmPassword');
            
            if (passwordRequired) passwordRequired.style.display = 'none';
            if (confirmPasswordRequired) confirmPasswordRequired.style.display = 'none';
            if (passwordInput) passwordInput.removeAttribute('required');
            if (confirmPasswordInput) confirmPasswordInput.removeAttribute('required');
        } else {
            title.textContent = 'Novo Funcionário';
            
            const passwordRequired = document.getElementById('passwordRequired');
            const confirmPasswordRequired = document.getElementById('confirmPasswordRequired');
            const passwordInput = document.getElementById('password');
            const confirmPasswordInput = document.getElementById('confirmPassword');
            
            if (passwordRequired) passwordRequired.style.display = 'inline';
            if (confirmPasswordRequired) confirmPasswordRequired.style.display = 'inline';
            if (passwordInput) passwordInput.setAttribute('required', 'required');
            if (confirmPasswordInput) confirmPasswordInput.setAttribute('required', 'required');
        }
        
        modal.style.display = 'block';
    }

    async loadFuncionarioData(id) {
        try {
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/fd/id/${id}`);
            
            if (response.ok) {
                const funcionario = await response.json();
                console.log('Dados do funcionário carregados:', funcionario);
                this.populateForm(funcionario);
            } else {
                throw new Error('Erro ao carregar dados do funcionário');
            }
        } catch (error) {
            console.error('Erro:', error);
            this.showAlert('Erro ao carregar dados do funcionário', 'error');
        }
    }

    populateForm(funcionario) {
        const userId = document.getElementById('userId');
        const name = document.getElementById('name');
        const email = document.getElementById('email');
        const phone = document.getElementById('phone');
        const role = document.getElementById('role');
        
        if (userId) userId.value = funcionario.id || '';
        if (name) name.value = funcionario.name || '';
        if (email) email.value = funcionario.email || '';
        if (phone) phone.value = funcionario.phone || '';
        
        const roles = funcionario.roles ? Array.from(funcionario.roles) : [];
        const mainRole = roles.length > 0 ? roles[0] : { roleStatus: 'BASIC' };
        if (role) role.value = mainRole.roleStatus;
        
        // Update role badge
        this.updateRoleBadge(mainRole.roleStatus);
    }

    clearErrors() {
        const errorElements = document.querySelectorAll('.error-message');
        errorElements.forEach(el => {
            if (el) el.textContent = '';
        });
        
        const inputs = document.querySelectorAll('.form-control');
        inputs.forEach(input => {
            if (input) input.classList.remove('error');
        });
    }

    validateForm() {
        let isValid = true;
        this.clearErrors();

        const name = document.getElementById('name')?.value.trim();
        if (!name) {
            this.showError('nameError', 'Nome é obrigatório');
            isValid = false;
        }

        const email = document.getElementById('email')?.value.trim();
        if (!email) {
            this.showError('emailError', 'Email é obrigatório');
            isValid = false;
        } else if (!this.validateEmail(email)) {
            this.showError('emailError', 'Email inválido');
            isValid = false;
        }

        const isEdit = !!this.currentEditId;
        const password = document.getElementById('password')?.value;
        const confirmPassword = document.getElementById('confirmPassword')?.value;

        if (!isEdit && !password) {
            this.showError('passwordError', 'Senha é obrigatória');
            isValid = false;
        } else if (password && password.length < 6) {
            this.showError('passwordError', 'Senha deve ter no mínimo 6 caracteres');
            isValid = false;
        }

        if (!isEdit && !confirmPassword) {
            this.showError('confirmPasswordError', 'Confirme a senha');
            isValid = false;
        } else if (password && confirmPassword && password !== confirmPassword) {
            this.showError('confirmPasswordError', 'As senhas não coincidem');
            isValid = false;
        }

        return isValid;
    }

    validateEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    }

    showError(elementId, message) {
        const element = document.getElementById(elementId);
        const inputId = elementId.replace('Error', '');
        const input = document.getElementById(inputId);
        
        if (element) element.textContent = message;
        if (input) input.classList.add('error');
    }

    async saveFuncionario() {
        console.log('Método saveFuncionario chamado');
        
        if (!this.validateForm()) {
            console.log('Validação falhou');
            return;
        }

        const isEdit = !!this.currentEditId;
        const formData = this.getFormData();
        const roleValue = document.getElementById('role')?.value || 'BASIC';
        
        console.log('Salvando funcionário:', { isEdit, formData, roleValue });

        try {
            let response;
            
            if (isEdit) {
                // Para edição, atualizar dados do usuário
                const updateData = {
                    name: formData.name,
                    email: formData.email,
                    phone: formData.phone || null
                };
                
                if (formData.password) {
                    updateData.password = formData.password;
                }
                
                console.log('Dados para atualização:', updateData);
                
                response = await this.makeAuthenticatedRequest(
                    `${this.baseUrl}/up/${this.currentEditId}`,
                    {
                        method: 'PUT',
                        body: JSON.stringify(updateData)
                    }
                );
                
                // Se a atualização foi bem sucedida, atualizar role
                if (response.ok) {
                    // Buscar o funcionário atualizado para pegar o nome
                    const funcionarioAtualizado = await this.loadFuncionarioDataSilent(this.currentEditId);
                    const userName = funcionarioAtualizado.name;
                    const roleAtual = funcionarioAtualizado.roles && funcionarioAtualizado.roles.length > 0 ? 
                        funcionarioAtualizado.roles[0].roleStatus : 'BASIC';
                    
                    if (roleAtual !== roleValue) {
                        console.log('Atualizando role de', roleAtual, 'para', roleValue);
                        // Tentar diferentes formatos para o update de role
                        try {
                            // Formato 1: JSON
                            const roleUpdateResponse = await fetch(`${this.baseUrl}/up/role`, {
                                method: 'PUT',
                                headers: {
                                    'Authorization': `Bearer ${this.token}`,
                                    'Content-Type': 'application/json'
                                },
                                body: JSON.stringify({
                                    name: userName,
                                    nameRole: roleValue
                                })
                            });
                            
                            if (!roleUpdateResponse.ok) {
                                console.error('Falha ao atualizar role com JSON, tentando form-data...');
                                
                                // Formato 2: Form data
                                const formDataRole = new FormData();
                                formDataRole.append('name', userName);
                                formDataRole.append('nameRole', roleValue);
                                
                                const roleUpdateResponse2 = await fetch(`${this.baseUrl}/up/role`, {
                                    method: 'PUT',
                                    headers: {
                                        'Authorization': `Bearer ${this.token}`
                                    },
                                    body: formDataRole
                                });
                                
                                if (!roleUpdateResponse2.ok) {
                                    throw new Error('Falha ao atualizar role');
                                }
                            }
                        } catch (roleError) {
                            console.error('Erro ao atualizar role:', roleError);
                            // Não falhar a operação principal se a atualização de role falhar
                            this.showAlert('Funcionário atualizado, mas houve erro ao atualizar o cargo', 'warning');
                        }
                    }
                }
            } else {
                // Para novo usuário, usar CreateUserDto
                const createDto = {
                    name: formData.name,
                    email: formData.email,
                    password: formData.password,
                    phone: formData.phone || null,
                    roleName: roleValue
                };
                
                console.log('Dados para criação:', createDto);
                
                response = await this.makeAuthenticatedRequest(
                    `${this.baseUrl}/is`,
                    {
                        method: 'POST',
                        body: JSON.stringify(createDto)
                    }
                );
            }

            if (response && response.ok) {
                const result = await response.json();
                console.log('Resposta do servidor:', result);
                
                this.showAlert(
                    `Funcionário ${isEdit ? 'atualizado' : 'cadastrado'} com sucesso!`,
                    'success'
                );
                
                const modal = document.getElementById('funcionarioModal');
                if (modal) modal.style.display = 'none';
                
                await this.loadFuncionarios();
            } else if (response) {
                const errorText = await response.text();
                console.error('Erro na resposta:', errorText);
                throw new Error(errorText || 'Erro ao salvar funcionário');
            }
        } catch (error) {
            console.error('Erro no saveFuncionario:', error);
            this.showAlert(`Erro ao salvar funcionário: ${error.message}`, 'error');
        }
    }
    
    async loadFuncionarioDataSilent(id) {
        try {
            const response = await fetch(`${this.baseUrl}/fd/id/${id}`, {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
            if (response.ok) {
                return await response.json();
            }
        } catch (error) {
            console.error('Erro ao carregar dados do funcionário:', error);
        }
        return null;
    }

    getFormData() {
        const name = document.getElementById('name')?.value.trim() || '';
        const email = document.getElementById('email')?.value.trim() || '';
        const phone = document.getElementById('phone')?.value.trim() || '';
        const password = document.getElementById('password')?.value || '';
        
        const formData = {
            name: name,
            email: email,
            phone: phone || null
        };

        if (password) {
            formData.password = password;
        }

        console.log('Dados do formulário:', formData);
        return formData;
    }

    openDeleteModal(id, name) {
        this.currentEditId = id;
        const modal = document.getElementById('confirmModal');
        const deleteUserName = document.getElementById('deleteUserName');
        
        if (modal && deleteUserName) {
            deleteUserName.textContent = name;
            modal.style.display = 'block';
        }
    }

    async deleteFuncionario() {
        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/dl/${this.currentEditId}`,
                {
                    method: 'DELETE'
                }
            );

            if (response.ok) {
                this.showAlert('Funcionário excluído com sucesso!', 'success');
                const modal = document.getElementById('confirmModal');
                if (modal) modal.style.display = 'none';
                await this.loadFuncionarios();
            } else {
                const errorText = await response.text();
                throw new Error(errorText || 'Erro ao excluir funcionário');
            }
        } catch (error) {
            console.error('Erro:', error);
            this.showAlert('Erro ao excluir funcionário: ' + error.message, 'error');
        }
    }

    showAlert(message, type = 'info') {
        const existingAlerts = document.querySelectorAll('.custom-alert');
        existingAlerts.forEach(alert => alert.remove());
        
        const alert = document.createElement('div');
        alert.className = `custom-alert alert-${type}`;
        alert.innerHTML = `
            <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'}"></i>
            <span>${message}</span>
        `;

        alert.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 1rem 1.5rem;
            border-radius: 8px;
            display: flex;
            align-items: center;
            gap: 0.75rem;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            z-index: 9999;
            animation: slideIn 0.3s ease;
            ${type === 'success' ? 
                'background: linear-gradient(135deg, #90EE90, #A8E6A8); color: #2d5016; border-left: 4px solid #28a745;' : 
                'background: linear-gradient(135deg, #f8d7da, #f5c6cb); color: #721c24; border-left: 4px solid #dc3545;'}
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
        
        if (!document.querySelector('#alert-animations')) {
            const style = document.createElement('style');
            style.id = 'alert-animations';
            style.textContent = `
                @keyframes slideIn {
                    from { transform: translateX(100%); opacity: 0; }
                    to { transform: translateX(0); opacity: 1; }
                }
                @keyframes slideOut {
                    from { transform: translateX(0); opacity: 1; }
                    to { transform: translateX(100%); opacity: 0; }
                }
            `;
            document.head.appendChild(style);
        }
    }
}

// Inicializar quando o DOM estiver pronto
document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM carregado, inicializando FuncionarioManager...');
    window.funcionarioManager = new FuncionarioManager();
});