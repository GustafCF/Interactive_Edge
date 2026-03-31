class ReservationManager {
    constructor() {
        this.baseUrl = '/reserve';
        this.token = localStorage.getItem('jwtToken');
        this.currentReservation = null;
        this.currentReservationId = null;
        this.selectedDates = new Set();
        this.currentStep = 1;
        this.guests = [];
        this.isReservationListExpanded = false;
        this.allReservations = [];
        this.currentReservationData = {
            guests: [],
            roomNumber: null,
            dates: []
        };
        this.initEventListeners();
        this.checkAuthentication();
        this.loadReservations();
        this.setupDateInputs();
    }

    setupDateInputs() {
        const today = new Date();
        const minDate = this.formatDateForInput(today);
        
        const dateInputs = document.querySelectorAll('input[type="date"]');
        dateInputs.forEach(input => {
            input.min = minDate;
        });
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

        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => this.searchReservations(e.target.value));
        }
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
        
        console.log('🌐 Fazendo requisição para:', url);
        console.log('⚙️ Opções:', {
            method: mergedOptions.method || 'GET',
            headers: mergedOptions.headers,
            body: mergedOptions.body ? JSON.parse(mergedOptions.body) : 'Sem body'
        });
        
        try {
            const response = await fetch(url, mergedOptions);
            
            console.log('📨 Resposta recebida:', {
                status: response.status,
                statusText: response.statusText,
                ok: response.ok,
                url: response.url
            });
            
            if (response.status === 401 || response.status === 403) {
                this.logout();
                throw new Error('Sessão expirada. Faça login novamente.');
            }
            
            return response;
        } catch (error) {
            console.error('💥 Erro na requisição:', error);
            console.error('🔗 URL da requisição:', url);
            throw error;
        }
    }

    // NOVA FUNÇÃO: Alternar entre lista expandida e recolhida
    toggleReservationList() {
        const container = document.getElementById('reservationListContainer');
        const button = document.querySelector('.btn-show-all');
        
        if (this.isReservationListExpanded) {
            // Recolher a lista
            container.classList.remove('expanded');
            container.classList.add('collapsed');
            button.textContent = 'Mostrar Todos';
            this.isReservationListExpanded = false;
        } else {
            // Expandir a lista
            container.classList.remove('collapsed');
            container.classList.add('expanded');
            button.textContent = 'Mostrar Menos';
            this.isReservationListExpanded = true;
        }
        
        // Re-renderizar as reservas com o novo estado
        this.displayReservations(this.allReservations);
    }

    async loadReservations() {
        try {
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/all`);
            
            if (response.ok) {
                const reservations = await response.json();
                
                // ✅ DEBUG: Verificar cálculo para cada reserva
                reservations.forEach((reserve, index) => {
                    const calculatedTotal = this.calculateReservationTotal(reserve);
                    console.log(`💰 Reserva #${reserve.id}: R$ ${calculatedTotal.toFixed(2)}`, {
                        initialValue: reserve.initialValue,
                        customValue: reserve.customValue,
                        useCustomValue: reserve.useCustomValue,
                        days: reserve.reservedDays?.size || reserve.reservedDays?.length,
                        guests: reserve.guest?.size || reserve.guest?.length,
                        calculatedBackend: reserve.calculatedTotal,
                        calculatedFrontend: calculatedTotal
                    });
                });
                
                this.allReservations = reservations;
                this.displayReservations(reservations);
            } else {
                throw new Error('Erro ao carregar reservas');
            }
        } catch (error) {
            console.error('Erro ao carregar reservas:', error);
            this.showAlert('Erro ao carregar lista de reservas', 'error');
        }
    }

    // ✅ MÉTODO NOVO: Calcular o total da reserva no frontend
    calculateReservationTotal(reservation) {
        try {
            // Se já veio calculado do backend, usa esse valor
            if (reservation.calculatedTotal !== undefined && reservation.calculatedTotal !== null) {
                return parseFloat(reservation.calculatedTotal);
            }
            
            // Se não, calcula manualmente baseado na lógica do backend
            const useCustomValue = reservation.useCustomValue === true;
            const customValue = reservation.customValue;
            
            // Se está usando valor customizado, retorna ele
            if (useCustomValue && customValue !== null && customValue !== undefined) {
                return parseFloat(customValue);
            }
            
            // Calcula baseado na lógica do backend
            const baseValue = reservation.initialValue || 0;
            const numberOfDays = reservation.reservedDays ? 
                (Array.isArray(reservation.reservedDays) ? reservation.reservedDays.length : reservation.reservedDays.size) : 0;
            const numberOfGuests = reservation.guest ? 
                (Array.isArray(reservation.guest) ? reservation.guest.length : reservation.guest.size) : 0;
            const extraGuests = Math.max(0, numberOfGuests - 1);
            const extraGuestFee = reservation.extraGuestFee || 20.00;
            
            // Cálculo: (valor base × número de dias) + (taxa extra × hóspedes extras × dias)
            const dailyTotal = baseValue * numberOfDays;
            const extraFees = extraGuestFee * extraGuests * numberOfDays;
            
            return dailyTotal + extraFees;
            
        } catch (error) {
            console.error('Erro ao calcular total da reserva:', error, reservation);
            return 0;
        }
    }

    displayReservations(reservations) {
        const container = document.getElementById('reservationList');
        if (!container) return;

        this.allReservations = reservations;

        if (reservations.length === 0) {
            container.innerHTML = '<p>Nenhuma reserva encontrada</p>';
            
            const buttonContainer = document.querySelector('.show-all-container');
            if (buttonContainer) {
                buttonContainer.style.display = 'none';
            }
            return;
        }

        const displayReservations = this.isReservationListExpanded ? 
            reservations : 
            reservations.slice(0, 5);

        const html = `
            <table class="table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Hóspedes</th>
                        <th>Quartos</th>
                        <th>Datas</th>
                        <th>Valor</th>
                        <th>Status</th>
                        <th>Check-in/out</th>
                        <th>Ações</th>
                    </tr>
                </thead>
                <tbody>
                    ${displayReservations.map(reservation => {
                        // ✅ CALCULA O VALOR DINAMICAMENTE NO FRONTEND
                        const totalValue = this.calculateReservationTotal(reservation);
                        const numberOfGuests = reservation.guest ? 
                            (Array.isArray(reservation.guest) ? reservation.guest.length : reservation.guest.size) : 0;
                        const extraGuests = Math.max(0, numberOfGuests - 1);
                        
                        return `
                            <tr>
                                <td>${reservation.id}</td>
                                <td>
                                    ${this.formatGuests(reservation.guest)}
                                </td>
                                <td>
                                    ${reservation.rooms && reservation.rooms.length > 0 ? 
                                        reservation.rooms.map(r => r.number).join(', ') : 
                                        'N/A'}
                                </td>
                                <td>
                                    <div class="dates-display">
                                        ${this.formatReservationDates(reservation.reservedDays)}
                                    </div>
                                </td>
                                <td>
                                    <strong>R$ ${totalValue.toFixed(2)}</strong>
                                    ${extraGuests > 0 ? 
                                        `<br><small style="color: #666;">(${extraGuests} hóspede(s) extra)</small>` : 
                                        ''}
                                    ${reservation.useCustomValue ? 
                                        `<br><small style="color: #ff9800;">💰 Valor personalizado</small>` : 
                                        ''}
                                </td>
                                <td>
                                    <span class="badge badge-${reservation.reserveStatus ? reservation.reserveStatus.toLowerCase() : 'unknown'}">
                                        ${reservation.reserveStatus || 'N/A'}
                                    </span>
                                </td>
                                <td>
                                    ${reservation.checkIn && reservation.checkIn.length > 0 ? '✅' : '❌'} /
                                    ${reservation.checkOut && reservation.checkOut.length > 0 ? '✅' : '❌'}
                                </td>
                                <td>
                                    <button class="btn btn-sm btn-info" onclick="reservationManager.manageReservation(${reservation.id})">
                                        👁️ Gerenciar
                                    </button>
                                    <button class="btn btn-sm btn-danger" onclick="reservationManager.deleteReservation(${reservation.id})" 
                                            style="margin-left: 0.25rem;">
                                        🗑️
                                    </button>
                                </td>
                            </tr>
                        `;
                    }).join('')}
                </tbody>
            </table>
            ${!this.isReservationListExpanded && reservations.length > 5 ? 
                `<div class="reservation-count-info">
                    <strong>${reservations.length - 5} reservas ocultas</strong><br>
                    <small>Clique em "Mostrar Todos" para ver todas as ${reservations.length} reservas</small>
                </div>` : 
                ''}
        `;

        container.innerHTML = html;

        const button = document.querySelector('.btn-show-all');
        const buttonContainer = document.querySelector('.show-all-container');
        if (button && buttonContainer) {
            button.textContent = this.isReservationListExpanded ? 'Mostrar Menos' : 'Mostrar Todos';
            buttonContainer.style.display = reservations.length > 5 ? 'block' : 'none';
        }
    }

    formatGuests(guests) {
        if (!guests || guests.length === 0) return 'N/A';
        
        const guestArray = Array.isArray(guests) ? guests : Array.from(guests);
        
        if (guestArray.length === 0) return 'N/A';
        
        const displayGuests = guestArray.slice(0, 2).map(guest => 
            guest.name ? this.escapeHtml(guest.name) : 'N/A'
        );
        
        let result = displayGuests.join(', ');
        
        if (guestArray.length > 2) {
            result += ` +${guestArray.length - 2}`;
        }
        
        return result;
    }

    formatReservationDates(dates) {
        if (!dates || dates.length === 0) return 'N/A';
        
        try {
            const datesArray = Array.isArray(dates) ? dates : Array.from(dates);
            
            if (datesArray.length === 0) return 'N/A';
            
            const sortedDates = datesArray.sort((a, b) => {
                const dateA = this.parseDate(a);
                const dateB = this.parseDate(b);
                return dateA - dateB;
            });

            const displayDates = sortedDates.slice(0, 3).map(date => {
                const parsedDate = this.parseDate(date);
                return `<span class="date-chip">${this.formatDate(parsedDate)}</span>`;
            });
            
            let result = displayDates.join('');
            
            if (sortedDates.length > 3) {
                result += `<span class="date-chip">+${sortedDates.length - 3}</span>`;
            }
            
            return result;
        } catch (error) {
            console.error('Erro ao formatar datas:', error, dates);
            return 'Erro nas datas';
        }
    }

    async searchReservations(searchTerm) {
        const resultsContainer = document.getElementById('searchResults');
        if (!resultsContainer) return;

        if (searchTerm.length < 1) {
            resultsContainer.innerHTML = '';
            return;
        }

        try {
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/all`);
            
            if (response.ok) {
                const allReservations = await response.json();
                const filteredReservations = allReservations.filter(reservation => 
                    reservation.id.toString().includes(searchTerm) ||
                    (reservation.guest && 
                     Array.from(reservation.guest).some(guest => 
                        guest.name && guest.name.toLowerCase().includes(searchTerm.toLowerCase())
                    ))
                );

                if (filteredReservations.length === 0) {
                    resultsContainer.innerHTML = '<p>Nenhuma reserva encontrada</p>';
                    return;
                }

                const html = filteredReservations.map(reservation => {
                    const totalValue = this.calculateReservationTotal(reservation);
                    return `
                        <div class="search-result-item">
                            <strong>Reserva #${reservation.id}</strong><br>
                            <strong>Hóspedes:</strong> ${this.formatGuests(reservation.guest)}<br>
                            <strong>Quarto:</strong> ${reservation.rooms && reservation.rooms.length > 0 ? 
                                reservation.rooms.map(r => r.number).join(', ') : 
                                'N/A'}<br>
                            <strong>Valor:</strong> R$ ${totalValue.toFixed(2)}<br>
                            <strong>Status:</strong> <span class="badge badge-${reservation.reserveStatus ? reservation.reserveStatus.toLowerCase() : 'unknown'}">
                                ${reservation.reserveStatus || 'N/A'}
                            </span>
                            <div style="margin-top: 8px;">
                                <button class="btn btn-sm btn-info" onclick="reservationManager.manageReservation(${reservation.id})">
                                    👁️ Gerenciar
                                </button>
                                <button class="btn btn-sm btn-primary" onclick="reservationManager.viewReservationDetails(${reservation.id})">
                                    📋 Detalhes
                                </button>
                                <button class="btn btn-sm btn-danger" onclick="reservationManager.deleteReservation(${reservation.id})">
                                    🗑️ Excluir
                                </button>
                            </div>
                        </div>
                    `;
                }).join('');

                resultsContainer.innerHTML = html;
            }
        } catch (error) {
            console.error('Erro na busca:', error);
            resultsContainer.innerHTML = '<p>Erro ao buscar reservas</p>';
        }
    }

    async loadValueDetails(reservationId) {
        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/${reservationId}/value-details`
            );
            
            if (response.ok) {
                return await response.json();
            } else {
                throw new Error('Erro ao carregar detalhes do valor');
            }
        } catch (error) {
            console.error('Erro ao carregar detalhes do valor:', error);
            this.showAlert('Erro ao carregar detalhes do valor', 'error');
            return null;
        }
    }

    async updateValueSection(reservationId) {
        try {
            const valueDetails = await this.loadValueDetails(reservationId);
            if (!valueDetails) return;

            const valueSection = document.getElementById('valueControlSection');
            if (!valueSection) return;

            const isCustomValue = valueDetails.useCustomValue === true;
            
            valueSection.innerHTML = `
                <div class="value-display">
                    💰 Valor Total: R$ ${valueDetails.calculatedTotal.toFixed(2)}
                </div>

                <div class="value-breakdown">
                    <h5>📊 Detalhamento do Valor</h5>
                    <div class="breakdown-item">
                        <span>Valor base do quarto:</span>
                        <span>R$ ${valueDetails.roomBaseValue.toFixed(2)}</span>
                    </div>
                    <div class="breakdown-item">
                        <span>Número de dias:</span>
                        <span>${valueDetails.numberOfDays}</span>
                    </div>
                    <div class="breakdown-item">
                        <span>Valor base total:</span>
                        <span>R$ ${(valueDetails.roomBaseValue * valueDetails.numberOfDays).toFixed(2)}</span>
                    </div>
                    <div class="breakdown-item">
                        <span>Hóspedes extras (${valueDetails.numberOfExtraGuests}):</span>
                        <span>R$ ${valueDetails.extraGuestFee.toFixed(2)} × ${valueDetails.numberOfExtraGuests} × ${valueDetails.numberOfDays} dias</span>
                    </div>
                    ${valueDetails.calculationBreakdown ? `
                        <div class="breakdown-item">
                            <span>Subtotal diária:</span>
                            <span>R$ ${valueDetails.calculationBreakdown.dailyTotal.toFixed(2)}</span>
                        </div>
                        <div class="breakdown-item">
                            <span>Taxas extras (hóspedes × dias):</span>
                            <span>R$ ${valueDetails.calculationBreakdown.extraFees.toFixed(2)}</span>
                        </div>
                    ` : ''}
                    <div class="breakdown-item">
                        <strong>Total:</strong>
                        <strong>R$ ${valueDetails.calculatedTotal.toFixed(2)}</strong>
                    </div>
                    ${isCustomValue ? `
                        <div style="margin-top: 0.5rem; padding: 0.5rem; background: #fff3cd; border-radius: 4px; text-align: center;">
                            ⚠️ Valor personalizado ativo
                        </div>
                    ` : ''}
                </div>

                <div class="value-controls">
                    <div class="value-control-group">
                        <input type="number" 
                               step="0.01" 
                               id="customValueInput" 
                               class="form-control value-input" 
                               placeholder="Valor personalizado"
                               value="${isCustomValue ? valueDetails.customValue.toFixed(2) : ''}">
                        <button class="btn btn-warning btn-sm" onclick="reservationManager.setCustomValue(${reservationId})">
                            💰 Personalizar
                        </button>
                    </div>
                    
                    <div class="value-control-group">
                        <input type="number" 
                               step="0.01" 
                               id="extraGuestFeeInput" 
                               class="form-control value-input" 
                               placeholder="Taxa por hóspede extra/dia"
                               value="${valueDetails.extraGuestFee.toFixed(2)}">
                        <button class="btn btn-info btn-sm" onclick="reservationManager.updateExtraGuestFee(${reservationId})">
                            👥 Atualizar Taxa
                        </button>
                    </div>
                </div>

                <div class="quick-actions" style="margin-top: 1rem; justify-content: center;">
                    ${isCustomValue ? `
                        <button class="btn btn-success btn-sm" onclick="reservationManager.setAutoValue(${reservationId})">
                            🔄 Voltar ao Cálculo Automático
                        </button>
                    ` : `
                        <button class="btn btn-secondary btn-sm" onclick="reservationManager.updateValueSection(${reservationId})">
                            🔄 Atualizar Cálculo
                        </button>
                    `}
                </div>
            `;

        } catch (error) {
            console.error('Erro ao atualizar seção de valores:', error);
        }
    }

    async setCustomValue(reservationId) {
        const customValueInput = document.getElementById('customValueInput');
        const customValue = parseFloat(customValueInput.value);

        if (isNaN(customValue) || customValue <= 0) {
            this.showAlert('Por favor, insira um valor válido maior que zero', 'error');
            return;
        }

        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/${reservationId}/custom-value`,
                {
                    method: 'PUT',
                    body: JSON.stringify(customValue)
                }
            );

            if (response.ok) {
                this.showAlert('Valor personalizado definido com sucesso!', 'success');
                await this.updateValueSection(reservationId);
                await this.manageReservation(reservationId);
                await this.loadReservations(); // ✅ Recarrega a lista para atualizar os valores
            } else {
                throw new Error('Erro ao definir valor personalizado');
            }
        } catch (error) {
            console.error('Erro ao definir valor personalizado:', error);
            this.showAlert('Erro ao definir valor personalizado: ' + error.message, 'error');
        }
    }

    async setAutoValue(reservationId) {
        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/${reservationId}/auto-value`,
                {
                    method: 'PUT'
                }
            );

            if (response.ok) {
                this.showAlert('Cálculo automático ativado com sucesso!', 'success');
                await this.updateValueSection(reservationId);
                await this.manageReservation(reservationId);
                await this.loadReservations(); // ✅ Recarrega a lista para atualizar os valores
            } else {
                throw new Error('Erro ao ativar cálculo automático');
            }
        } catch (error) {
            console.error('Erro ao ativar cálculo automático:', error);
            this.showAlert('Erro ao ativar cálculo automático: ' + error.message, 'error');
        }
    }

    async updateExtraGuestFee(reservationId) {
        const extraGuestFeeInput = document.getElementById('extraGuestFeeInput');
        const newFee = parseFloat(extraGuestFeeInput.value);

        if (isNaN(newFee) || newFee < 0) {
            this.showAlert('Por favor, insira uma taxa válida (maior ou igual a zero)', 'error');
            return;
        }

        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/${reservationId}/extra-guest-fee`,
                {
                    method: 'PUT',
                    body: JSON.stringify(newFee)
                }
            );

            if (response.ok) {
                this.showAlert(`Taxa de hóspede extra atualizada para R$ ${newFee.toFixed(2)}/dia!`, 'success');
                await this.updateValueSection(reservationId);
                await this.manageReservation(reservationId);
                await this.loadReservations(); // ✅ Recarrega a lista para atualizar os valores
            } else {
                throw new Error('Erro ao atualizar taxa de hóspede extra');
            }
        } catch (error) {
            console.error('Erro ao atualizar taxa de hóspede extra:', error);
            this.showAlert('Erro ao atualizar taxa de hóspede extra: ' + error.message, 'error');
        }
    }

    // FUNÇÕES DO FLUXO DE CRIAÇÃO
    goToStep(step) {
        if (step > this.currentStep) {
            if (!this.validateCurrentStep()) {
                return;
            }
        }

        document.querySelectorAll('.step').forEach((stepElement, index) => {
            const stepNumber = index + 1;
            if (stepNumber < step) {
                stepElement.className = 'step completed';
            } else if (stepNumber === step) {
                stepElement.className = 'step active';
            } else {
                stepElement.className = 'step';
            }
        });

        document.querySelectorAll('.flow-section').forEach((section, index) => {
            section.style.display = (index + 1 === step) ? 'block' : 'none';
        });

        this.currentStep = step;

        if (step === 4) {
            this.updateReservationSummary();
        }
    }

    validateCurrentStep() {
        switch (this.currentStep) {
            case 1:
                if (this.guests.length === 0) {
                    this.showAlert('Por favor, cadastre pelo menos um hóspede', 'error');
                    return false;
                }
                return true;
            
            case 2:
                const roomNumber = parseInt(document.getElementById('roomNumber').value);
                if (!roomNumber) {
                    this.showAlert('Por favor, selecione um número de quarto', 'error');
                    return false;
                }
                this.currentReservationData.roomNumber = roomNumber;
                return true;
            
            case 3:
                if (this.selectedDates.size === 0) {
                    this.showAlert('Por favor, selecione pelo menos uma data', 'error');
                    return false;
                }
                this.currentReservationData.dates = Array.from(this.selectedDates);
                return true;
            
            default:
                return true;
        }
    }

    addGuestToList() {
        const name = document.getElementById('guestName').value.trim();
        const rg = document.getElementById('guestRg').value.trim();
        const phone = document.getElementById('guestPhone').value.trim();
        const email = document.getElementById('guestEmail').value.trim();

        if (!name || !rg || !phone) {
            this.showAlert('Por favor, preencha pelo menos nome, RG e telefone', 'error');
            return;
        }

        const guest = {
            id: Date.now(),
            name: name,
            rg: rg,
            phone: phone,
            email: email || 'Não informado'
        };

        this.guests.push(guest);
        this.updateGuestList();
        
        document.getElementById('guestName').value = '';
        document.getElementById('guestRg').value = '';
        document.getElementById('guestPhone').value = '';
        document.getElementById('guestEmail').value = '';

        this.showAlert('Hóspede adicionado com sucesso!', 'success');
    }

    updateGuestList() {
        const container = document.getElementById('guestList');
        
        if (this.guests.length === 0) {
            container.innerHTML = '<p style="color: #666; font-style: italic; text-align: center; padding: 2rem;">Nenhum hóspede cadastrado ainda</p>';
            return;
        }

        const html = this.guests.map(guest => `
            <div class="guest-list-item">
                <div class="guest-info">
                    <div class="guest-name">${this.escapeHtml(guest.name)}</div>
                    <div class="guest-details">
                        RG: ${this.escapeHtml(guest.rg)} | 
                        Tel: ${this.escapeHtml(guest.phone)} | 
                        Email: ${this.escapeHtml(guest.email)}
                    </div>
                </div>
                <div class="guest-actions">
                    <button class="btn btn-sm btn-danger" onclick="reservationManager.removeGuestFromList(${guest.id})">
                        ×
                    </button>
                </div>
            </div>
        `).join('');

        container.innerHTML = html;
    }

    removeGuestFromList(guestId) {
        this.guests = this.guests.filter(guest => guest.id !== guestId);
        this.updateGuestList();
        this.showAlert('Hóspede removido da lista', 'info');
    }

    updateReservationSummary() {
        const guestsHtml = this.guests.map(guest => `
            <div style="margin-bottom: 0.5rem; padding: 0.5rem; background: #f8f9fa; border-radius: 4px;">
                <strong>${this.escapeHtml(guest.name)}</strong><br>
                <small>RG: ${this.escapeHtml(guest.rg)} | Tel: ${this.escapeHtml(guest.phone)}</small>
            </div>
        `).join('');
        document.getElementById('summaryGuests').innerHTML = guestsHtml;

        document.getElementById('summaryRoom').innerHTML = `
            <div style="padding: 0.5rem; background: #f8f9fa; border-radius: 4px;">
                <strong>Quarto ${this.currentReservationData.roomNumber}</strong>
            </div>
        `;

        const datesHtml = Array.from(this.selectedDates)
            .sort()
            .map(date => {
                const formattedDate = this.formatDate(this.parseDate(date));
                return `<span class="date-chip">${formattedDate}</span>`;
            }).join('');
        
        document.getElementById('summaryDates').innerHTML = datesHtml || '<p style="color: #666;">Nenhuma data selecionada</p>';
    }

    // ✅ CORREÇÃO: Funções para seleção de datas com UTC
    addSingleDateToSelection() {
        const dateInput = document.getElementById('singleDateInput');
        const date = dateInput.value;
        
        if (!date) {
            this.showAlert('Por favor, selecione uma data', 'error');
            return;
        }

        this.selectedDates.add(date);
        this.updateSelectedDatesDisplay();
        dateInput.value = '';
        this.showAlert('Data adicionada com sucesso!', 'success');
    }

    addDateRangeToSelection() {
        const startDate = document.getElementById('periodStartDate').value;
        const endDate = document.getElementById('periodEndDate').value;
        
        if (!startDate || !endDate) {
            this.showAlert('Por favor, selecione ambas as datas do período', 'error');
            return;
        }

        // ✅ CORREÇÃO: Usar UTC para evitar problemas de fuso horário
        const start = new Date(startDate + 'T12:00:00Z'); // Meio-dia UTC
        const end = new Date(endDate + 'T12:00:00Z');
        
        if (start >= end) {
            this.showAlert('A data final deve ser posterior à data inicial', 'error');
            return;
        }

        const dates = this.getDatesBetween(startDate, endDate);
        console.log('📅 Datas geradas para o período:', dates);
        
        dates.forEach(date => this.selectedDates.add(date));
        
        this.updateSelectedDatesDisplay();
        
        document.getElementById('periodStartDate').value = '';
        document.getElementById('periodEndDate').value = '';
        
        this.showAlert(`Período de ${dates.length} datas adicionado com sucesso!`, 'success');
    }

    removeDateFromSelection(dateToRemove) {
        this.selectedDates.delete(dateToRemove);
        this.updateSelectedDatesDisplay();
        this.showAlert('Data removida da seleção', 'info');
    }

    updateSelectedDatesDisplay() {
        const container = document.getElementById('selectedDatesList');
        
        if (this.selectedDates.size === 0) {
            container.innerHTML = '<p style="color: #666; font-style: italic;">Nenhuma data selecionada</p>';
            return;
        }

        const sortedDates = Array.from(this.selectedDates).sort();
        
        const html = sortedDates.map(date => {
            const formattedDate = this.formatDate(this.parseDate(date));
            return `
                <div class="selected-date-item">
                    <span>${formattedDate}</span>
                    <button class="btn btn-sm btn-danger" onclick="reservationManager.removeDateFromSelection('${date}')">
                        ×
                    </button>
                </div>
            `;
        }).join('');

        container.innerHTML = html;
    }

    // ✅ CORREÇÃO: Método getDatesBetween com UTC
    getDatesBetween(startDate, endDate) {
        const dates = [];
        
        // ✅ CORREÇÃO: Usar UTC para evitar problemas de fuso horário
        const start = new Date(startDate + 'T12:00:00Z');
        const end = new Date(endDate + 'T12:00:00Z');
        
        const currentDate = new Date(start);
        
        while (currentDate <= end) {
            // ✅ CORREÇÃO: Usar UTC para formatar a data
            const year = currentDate.getUTCFullYear();
            const month = String(currentDate.getUTCMonth() + 1).padStart(2, '0');
            const day = String(currentDate.getUTCDate()).padStart(2, '0');
            
            dates.push(`${year}-${month}-${day}`);
            currentDate.setUTCDate(currentDate.getUTCDate() + 1);
        }

        console.log('📅 Datas geradas (getDatesBetween):', dates);
        return dates;
    }

    clearDateSelection() {
        this.selectedDates.clear();
        this.updateSelectedDatesDisplay();
        this.showAlert('Todas as datas foram removidas', 'info');
    }

    // FUNÇÕES PARA MODAIS E FLUXO
    openCreateModal() {
        this.resetCreationFlow();
        document.getElementById('createReservationModal').style.display = 'block';
    }

    closeCreateModal() {
        document.getElementById('createReservationModal').style.display = 'none';
        this.resetCreationFlow();
    }

    resetCreationFlow() {
        this.currentStep = 1;
        this.guests = [];
        this.selectedDates.clear();
        this.currentReservationData = {
            guests: [],
            roomNumber: null,
            dates: []
        };

        this.goToStep(1);
        this.updateGuestList();
        this.updateSelectedDatesDisplay();
        
        document.getElementById('guestName').value = '';
        document.getElementById('guestRg').value = '';
        document.getElementById('guestPhone').value = '';
        document.getElementById('guestEmail').value = '';
        document.getElementById('roomNumber').value = '';
    }

    // ✅ CORREÇÃO: Método createReservation usando lista de hóspedes
    async createReservation() {
        try {
            // Validações iniciais
            if (this.guests.length === 0) {
                this.showAlert('Por favor, cadastre pelo menos um hóspede', 'error');
                this.goToStep(1);
                return;
            }

            if (!this.currentReservationData.roomNumber) {
                this.showAlert('Por favor, selecione um número de quarto', 'error');
                this.goToStep(2);
                return;
            }

            if (this.selectedDates.size === 0) {
                this.showAlert('Por favor, selecione pelo menos uma data', 'error');
                this.goToStep(3);
                return;
            }

            // ✅ CORREÇÃO: Converter TODOS os hóspedes para a estrutura do DTO
            const guestsData = this.guests.map(guest => ({
                name: guest.name,
                rg: guest.rg,
                phone: guest.phone,
                email: guest.email || ''
            }));
            
            // ✅ CORREÇÃO: Converter datas para o formato LocalDate
            const datesArray = Array.from(this.selectedDates).sort();
            
            console.log('📅 Datas que serão enviadas:', datesArray);
            console.log('👥 Hóspedes que serão enviados:', guestsData);
            console.log('🏠 Quarto:', this.currentReservationData.roomNumber);

            // ✅ CORREÇÃO: Usar o novo DTO com lista de hóspedes
            const reservationData = {
                guests: guestsData,  // ✅ AGORA é uma lista
                roomNumber: this.currentReservationData.roomNumber,
                dates: datesArray
            };

            console.log('📤 Enviando dados para criação de reserva:', reservationData);

            // ✅ CORREÇÃO: Usar o novo endpoint específico
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/create-with-guest`, {
                method: 'POST',
                body: JSON.stringify(reservationData)
            });

            if (response.ok) {
                const savedReservation = await response.json();
                console.log('✅ Reserva criada com sucesso:', savedReservation);
                
                this.showAlert(`Reserva criada com sucesso com ${this.guests.length} hóspedes!`, 'success');
                this.closeCreateModal();
                this.loadReservations();
                
            } else {
                const errorText = await response.text();
                console.error('❌ Resposta do servidor:', errorText);
                
                let errorMessage = 'Erro ao criar reserva';
                try {
                    const errorJson = JSON.parse(errorText);
                    errorMessage = errorJson.message || errorJson.error || errorText;
                } catch {
                    errorMessage = errorText || `Erro ${response.status}: ${response.statusText}`;
                }
                
                throw new Error(errorMessage);
            }

        } catch (error) {
            console.error('💥 Erro ao criar reserva:', error);
            
            let userMessage = error.message;
            if (error.message.includes('Resource not found')) {
                userMessage = 'Hóspede ou quarto não encontrado. Verifique se os dados estão corretos.';
            } else if (error.message.includes('already reserved')) {
                userMessage = 'Quarto já reservado para as datas selecionadas.';
            } else if (error.message.includes('DateTimeParseException')) {
                userMessage = 'Formato de data inválido.';
            } else if (error.message.includes('Pelo menos um hóspede')) {
                userMessage = 'É necessário cadastrar pelo menos um hóspede.';
            }
            
            this.showAlert('Erro ao criar reserva: ' + userMessage, 'error');
            
            // Mostrar detalhes técnicos no console para debug
            console.error('Detalhes técnicos do erro:', {
                message: error.message,
                stack: error.stack,
                guestCount: this.guests.length,
                roomNumber: this.currentReservationData.roomNumber,
                datesCount: this.selectedDates.size,
                dates: Array.from(this.selectedDates)
            });
        }
    }

    // ✅ CORREÇÃO: Métodos de formatação de data com UTC
    parseDate(dateString) {
        if (typeof dateString === 'string') {
            if (/^\d{4}-\d{2}-\d{2}$/.test(dateString)) {
                // ✅ CORREÇÃO: Usar UTC para parsing
                const [year, month, day] = dateString.split('-').map(Number);
                return new Date(Date.UTC(year, month - 1, day, 12, 0, 0)); // Meio-dia UTC
            }
            
            const parsed = new Date(dateString);
            if (!isNaN(parsed)) return parsed;
        }
        
        console.warn('Data inválida:', dateString);
        return new Date();
    }

    formatDate(date) {
        if (!(date instanceof Date) || isNaN(date)) {
            return 'Data inválida';
        }
        
        return date.toLocaleDateString('pt-BR');
    }

    formatDateForInput(date) {
        if (!(date instanceof Date) || isNaN(date)) {
            return '';
        }
        
        // ✅ CORREÇÃO: Usar UTC para evitar problemas de fuso horário
        const year = date.getUTCFullYear();
        const month = String(date.getUTCMonth() + 1).padStart(2, '0');
        const day = String(date.getUTCDate()).padStart(2, '0');
        
        return `${year}-${month}-${day}`;
    }

    // ✅ NOVO: Método para debug de datas
    debugDates(startDate, endDate) {
        console.log('🐛 DEBUG DATAS:');
        console.log('Data inicial (input):', startDate);
        console.log('Data final (input):', endDate);
        
        const dates = this.getDatesBetween(startDate, endDate);
        console.log('Datas finais geradas:', dates);
        
        return dates;
    }

    // ✅ NOVO: Método para testar o problema das datas
    testDateIssue() {
        console.log('🧪 TESTE DE DATAS:');
        
        // Simular o problema relatado
        const testStart = '2025-11-17';
        const testEnd = '2025-11-19';
        
        console.log('Datas de teste:', testStart, 'até', testEnd);
        
        const dates = this.getDatesBetween(testStart, testEnd);
        console.log('Datas geradas:', dates);
        
        // Verificar cada data individualmente
        dates.forEach(date => {
            const parsed = new Date(date + 'T12:00:00Z');
            console.log(`Data: ${date}, Local: ${parsed.toString()}, UTC: ${parsed.toUTCString()}`);
        });
        
        return dates;
    }

    // ✅ NOVO: Método para debug rápido
    quickDebug() {
        console.log('🚀 DEBUG RÁPIDO:');
        console.log('Datas selecionadas:', Array.from(this.selectedDates));
        console.log('Número de hóspedes:', this.guests.length);
        console.log('Quarto selecionado:', this.currentReservationData.roomNumber);
        console.log('Step atual:', this.currentStep);
        
        // Testar o problema das datas
        this.testDateIssue();
    }

    async viewReservationDetails(reservationId) {
        try {
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/find/${reservationId}`);
            
            if (response.ok) {
                const reservation = await response.json();
                this.showReservationDetails(reservation);
            } else {
                throw new Error('Erro ao carregar detalhes da reserva');
            }
        } catch (error) {
            console.error('Erro ao carregar detalhes:', error);
            this.showAlert('Erro ao carregar detalhes da reserva', 'error');
        }
    }

    showReservationDetails(reservation) {
        const totalValue = this.calculateReservationTotal(reservation);
        const extraGuests = reservation.guest ? (Array.isArray(reservation.guest) ? reservation.guest.length - 1 : reservation.guest.size - 1) : 0;
        const numberOfDays = reservation.reservedDays ? (Array.isArray(reservation.reservedDays) ? reservation.reservedDays.length : reservation.reservedDays.size) : 0;
        
        const html = `
            <div class="card">
                <h3>Detalhes da Reserva #${reservation.id}</h3>
                <div class="reservation-form">
                    <div class="form-group">
                        <strong>Status:</strong> 
                        <span class="badge badge-${reservation.reserveStatus ? reservation.reserveStatus.toLowerCase() : 'unknown'}">
                            ${reservation.reserveStatus || 'N/A'}
                        </span>
                    </div>
                    <div class="form-group">
                        <strong>Hóspedes:</strong><br>
                        ${this.formatGuestDetails(reservation.guest)}
                        ${extraGuests > 0 ? 
                            `<div style="margin-top: 0.5rem; padding: 0.25rem; background: #e7f3ff; border-radius: 4px;">
                                <small>💰 ${extraGuests} hóspede(s) extra - taxa aplicada por ${numberOfDays} dia(s)</small>
                            </div>` : 
                            ''}
                    </div>
                    <div class="form-group">
                        <strong>Quartos:</strong><br>
                        ${reservation.rooms && reservation.rooms.length > 0 ? 
                            reservation.rooms.map(room => `
                                <div class="guest-item">
                                    <span>Quarto ${room.number} - ${room.exclusiveRoom ? 'Exclusivo' : 'Compartilhado'} - R$ ${room.price ? parseFloat(room.price).toFixed(2) : '0.00'}/dia</span>
                                </div>
                            `).join('') : 
                            '<p>Nenhum quarto</p>'
                        }
                    </div>
                    <div class="form-group">
                        <strong>Datas Reservadas (${numberOfDays} dias):</strong><br>
                        <div class="dates-display">
                            ${this.formatReservationDates(reservation.reservedDays)}
                        </div>
                    </div>
                    <div class="form-group">
                        <strong>Valor Total:</strong> R$ ${totalValue.toFixed(2)}
                        ${reservation.useCustomValue ? 
                            `<br><small style="color: #ff9800;">💰 Valor personalizado</small>` : 
                            ''}
                    </div>
                    <div class="form-group">
                        <strong>Check-in:</strong> ${reservation.checkIn && reservation.checkIn.length > 0 ? '✅ Realizado' : '❌ Pendente'}
                    </div>
                    <div class="form-group">
                        <strong>Check-out:</strong> ${reservation.checkOut && reservation.checkOut.length > 0 ? '✅ Realizado' : '❌ Pendente'}
                    </div>
                </div>
            </div>
        `;

        const manageContent = document.getElementById('manageReservationContent');
        manageContent.innerHTML = html;
        document.getElementById('manageReservationModal').style.display = 'block';
    }

    formatGuestDetails(guests) {
        if (!guests || guests.length === 0) return '<p>Nenhum hóspede</p>';
        
        const guestArray = Array.isArray(guests) ? guests : Array.from(guests);
        
        return guestArray.map(guest => `
            <div class="guest-item">
                <span>${guest.name || 'N/A'} - ${guest.rg || 'N/A'} - ${guest.phone || 'N/A'}</span>
            </div>
        `).join('');
    }

    // FUNÇÕES DE GERENCIAMENTO DE RESERVAS
    async manageReservation(reservationId) {
        try {
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/find/${reservationId}`);
            
            if (response.ok) {
                const reservation = await response.json();
                this.currentReservation = reservation;
                this.currentReservationId = reservationId;
                this.openManageModal(reservation);
            } else {
                throw new Error('Erro ao carregar reserva');
            }
        } catch (error) {
            console.error('Erro ao carregar reserva:', error);
            this.showAlert('Erro ao carregar reserva', 'error');
        }
    }

    openManageModal(reservation) {
        const container = document.getElementById('manageReservationContent');
        const totalValue = this.calculateReservationTotal(reservation);
        
        const html = `
            <div>
                <div class="quick-stats">
                    <div class="stat-card">
                        <div class="stat-number">#${reservation.id}</div>
                        <div class="stat-label">ID da Reserva</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-number">${reservation.guest ? (Array.isArray(reservation.guest) ? reservation.guest.length : reservation.guest.size) : 0}</div>
                        <div class="stat-label">Hóspedes</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-number">${reservation.rooms ? reservation.rooms.length : 0}</div>
                        <div class="stat-label">Quartos</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-number">${reservation.reservedDays ? (Array.isArray(reservation.reservedDays) ? reservation.reservedDays.length : reservation.reservedDays.size) : 0}</div>
                        <div class="stat-label">Noites</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-number">R$ ${totalValue.toFixed(2)}</div>
                        <div class="stat-label">Valor Total</div>
                    </div>
                </div>

                <div class="management-section">
                    <h4 class="section-title">💰 Controle de Valores</h4>
                    <div id="valueControlSection">
                        <p>Carregando informações de valor...</p>
                    </div>
                </div>

                <div class="management-section">
                    <h4 class="section-title">📊 Status da Reserva</h4>
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <span class="status-badge status-${reservation.reserveStatus ? reservation.reserveStatus.toLowerCase() : 'unknown'}">
                                ${reservation.reserveStatus || 'N/A'}
                            </span>
                            <div style="margin-top: 0.5rem;">
                                <small>
                                    Check-in: ${reservation.checkIn && reservation.checkIn.length > 0 ? '✅ Realizado' : '❌ Pendente'} | 
                                    Check-out: ${reservation.checkOut && reservation.checkOut.length > 0 ? '✅ Realizado' : '❌ Pendente'}
                                </small>
                            </div>
                        </div>
                        <div class="quick-actions">
                            ${reservation.reserveStatus !== 'CANCELLED' && reservation.checkIn && reservation.checkIn.length === 0 ? `
                                <button class="btn btn-success btn-sm" onclick="reservationManager.checkIn(${reservation.id})">
                                    ✅ Check-in
                                </button>
                            ` : ''}
                            ${reservation.checkIn && reservation.checkIn.length > 0 && reservation.checkOut && reservation.checkOut.length === 0 ? `
                                <button class="btn btn-info btn-sm" onclick="reservationManager.checkOut(${reservation.id})">
                                    🏁 Check-out
                                </button>
                            ` : ''}
                            ${reservation.reserveStatus !== 'CANCELLED' ? `
                                <button class="btn btn-danger btn-sm" onclick="reservationManager.cancelReservation(${reservation.id})">
                                    ❌ Cancelar
                                </button>
                            ` : ''}
                            <button class="btn btn-danger btn-sm" onclick="reservationManager.deleteReservation(${reservation.id})">
                                🗑️ Excluir Reserva
                            </button>
                        </div>
                    </div>
                </div>

                <div class="management-section">
                    <h4 class="section-title">⚡ Ações Rápidas</h4>
                    <div class="action-grid">
                        <div class="action-card" onclick="reservationManager.openAddDatesModal(${reservation.id})">
                            <div class="action-icon">📅</div>
                            <div class="action-title">Adicionar Datas</div>
                            <div class="action-description">Incluir novas datas na reserva</div>
                        </div>
                        <div class="action-card" onclick="reservationManager.openAddRoomModal(${reservation.id})">
                            <div class="action-icon">🏠</div>
                            <div class="action-title">Adicionar Quarto</div>
                            <div class="action-description">Incluir outro quarto na reserva</div>
                        </div>
                        <div class="action-card" onclick="reservationManager.focusAddGuest(${reservation.id})">
                            <div class="action-icon">👥</div>
                            <div class="action-title">Adicionar Hóspede</div>
                            <div class="action-description">Incluir novo hóspede</div>
                        </div>
                        <div class="action-card" onclick="reservationManager.viewFullDetails(${reservation.id})">
                            <div class="action-icon">📋</div>
                            <div class="action-title">Ver Detalhes</div>
                            <div class="action-description">Visualizar informações completas</div>
                        </div>
                    </div>
                </div>

                <div class="management-section">
                    <h4 class="section-title">📅 Datas da Reserva</h4>
                    <div class="dates-management">
                        <div>
                            <h5>Datas Atuais</h5>
                            <div class="current-dates">
                                ${this.formatCurrentDates(reservation.reservedDays, reservation.id)}
                            </div>
                        </div>
                        <div>
                            <h5>Adicionar Datas</h5>
                            <div class="date-picker-group">
                                <div class="form-group" style="flex: 1;">
                                    <input type="date" id="quickAddDate" class="form-control">
                                </div>
                                <button class="btn btn-success btn-sm" onclick="reservationManager.quickAddDate(${reservation.id})">
                                    +
                                </button>
                            </div>
                            <small style="color: #666; display: block; margin-top: 0.5rem;">
                                Clique no botão "+" para adicionar rapidamente uma data
                            </small>
                        </div>
                    </div>
                </div>

                <div class="management-section">
                    <h4 class="section-title">👥 Hóspedes</h4>
                    <div class="guest-list" id="currentGuestList">
                        ${this.formatGuestList(reservation.guest, reservation.id)}
                    </div>
                    <div class="form-group" style="margin-top: 1rem;">
                        <div class="date-picker-group">
                            <input type="text" id="newGuestName" class="form-control" placeholder="Nome do novo hóspede" style="flex: 1;">
                            <button class="btn btn-success btn-sm" onclick="reservationManager.addGuest(${reservation.id})">
                                Adicionar
                            </button>
                        </div>
                    </div>
                </div>

                <div class="management-section">
                    <h4 class="section-title">🏠 Quartos</h4>
                    <div class="guest-list">
                        ${reservation.rooms && reservation.rooms.length > 0 ? 
                            reservation.rooms.map(room => `
                                <div class="guest-item">
                                    <span>Quarto ${room.number} - ${room.exclusiveRoom ? '🔄 Exclusivo' : '👥 Compartilhado'}</span>
                                    ${reservation.rooms.length > 1 ? `
                                        <button class="btn btn-sm btn-danger" onclick="reservationManager.removeRoom(${reservation.id}, ${room.number})">
                                            Remover
                                        </button>
                                    ` : '<small style="color: #666;">Quarto principal</small>'}
                                </div>
                            `).join('') : 
                            '<p>Nenhum quarto definido</p>'
                        }
                    </div>
                </div>
            </div>
        `;

        container.innerHTML = html;
        document.getElementById('manageReservationModal').style.display = 'block';
        
        this.updateValueSection(reservation.id);
    }

    formatCurrentDates(dates, reservationId) {
        if (!dates || (Array.isArray(dates) && dates.length === 0) || (!Array.isArray(dates) && dates.size === 0)) {
            return '<p>Nenhuma data definida</p>';
        }
        
        try {
            const datesArray = Array.isArray(dates) ? dates : Array.from(dates);
            const sortedDates = datesArray.sort((a, b) => {
                const dateA = this.parseDate(a);
                const dateB = this.parseDate(b);
                return dateA - dateB;
            });

            return sortedDates.map(date => {
                const parsedDate = this.parseDate(date);
                const formattedDate = this.formatDate(parsedDate);
                return `
                    <div class="date-item">
                        <span>${formattedDate}</span>
                        <button class="btn btn-sm btn-danger" onclick="reservationManager.removeDate(${reservationId}, '${date}')">
                            ×
                        </button>
                    </div>
                `;
            }).join('');
        } catch (error) {
            console.error('Erro ao formatar datas:', error);
            return '<p>Erro ao carregar datas</p>';
        }
    }

    formatGuestList(guests, reservationId) {
        if (!guests || guests.length === 0) return '<p>Nenhum hóspede</p>';
        
        const guestArray = Array.isArray(guests) ? guests : Array.from(guests);
        
        return guestArray.map(guest => `
            <div class="guest-item">
                <span>${guest.name || 'N/A'}</span>
                <button class="btn btn-sm btn-danger" onclick="reservationManager.removeGuest(${reservationId}, '${this.escapeHtml(guest.name)}')">
                    Remover
                </button>
            </div>
        `).join('');
    }

    closeManageModal() {
        document.getElementById('manageReservationModal').style.display = 'none';
        this.currentReservation = null;
        this.currentReservationId = null;
    }

    // FUNÇÕES AUXILIARES DO GERENCIAMENTO
    openAddDatesModal(reservationId) {
        this.currentReservationId = reservationId;
        document.getElementById('addDatesModal').style.display = 'block';
    }

    closeAddDatesModal() {
        document.getElementById('addDatesModal').style.display = 'none';
        document.getElementById('dateAvailabilityResult').innerHTML = '';
    }

    openAddRoomModal(reservationId) {
        this.currentReservationId = reservationId;
        document.getElementById('addRoomModal').style.display = 'block';
    }

    closeAddRoomModal() {
        document.getElementById('addRoomModal').style.display = 'none';
    }

    focusAddGuest(reservationId) {
        this.currentReservationId = reservationId;
        const newGuestInput = document.getElementById('newGuestName');
        if (newGuestInput) {
            newGuestInput.focus();
        }
    }

    async quickAddDate(reservationId) {
        const dateInput = document.getElementById('quickAddDate');
        const newDate = dateInput.value;

        if (!newDate) {
            this.showAlert('Por favor, selecione uma data', 'error');
            return;
        }

        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/${reservationId}/add-date`,
                {
                    method: 'PUT',
                    body: JSON.stringify(newDate)
                }
            );

            if (response.ok) {
                this.showAlert('Data adicionada com sucesso!', 'success');
                dateInput.value = '';
                await this.manageReservation(reservationId);
                await this.updateValueSection(reservationId);
                await this.loadReservations(); // ✅ Recarrega a lista para atualizar os valores
            } else {
                throw new Error('Erro ao adicionar data');
            }
        } catch (error) {
            console.error('Erro ao adicionar data:', error);
            this.showAlert('Erro ao adicionar data: ' + error.message, 'error');
        }
    }

    async removeDate(reservationId, dateToRemove) {
        if (!confirm(`Tem certeza que deseja remover a data ${this.formatDate(this.parseDate(dateToRemove))}?`)) {
            return;
        }

        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/remove-date/${reservationId}?date=${dateToRemove}`,
                {
                    method: 'PUT'
                }
            );

            if (response.ok) {
                this.showAlert('Data removida com sucesso!', 'success');
                await this.manageReservation(reservationId);
                await this.updateValueSection(reservationId);
                await this.loadReservations(); // ✅ Recarrega a lista para atualizar os valores
            } else {
                throw new Error('Erro ao remover data');
            }
        } catch (error) {
            console.error('Erro ao remover data:', error);
            this.showAlert('Erro ao remover data: ' + error.message, 'error');
        }
    }

    async addRoom() {
        const roomNumber = parseInt(document.getElementById('newRoomNumber').value);
        
        if (!roomNumber) {
            this.showAlert('Por favor, digite o número do quarto', 'error');
            return;
        }

        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/${this.currentReservationId}/add-room?roomNumber=${roomNumber}`,
                { method: 'PUT' }
            );

            if (response.ok) {
                this.showAlert('Quarto adicionado com sucesso!', 'success');
                this.closeAddRoomModal();
                await this.manageReservation(this.currentReservationId);
                await this.updateValueSection(this.currentReservationId);
                await this.loadReservations(); // ✅ Recarrega a lista para atualizar os valores
            } else {
                throw new Error('Erro ao adicionar quarto');
            }
        } catch (error) {
            console.error('Erro ao adicionar quarto:', error);
            this.showAlert('Erro ao adicionar quarto: ' + error.message, 'error');
        }
    }

    async removeRoom(reservationId, roomNumber) {
        if (!confirm(`Tem certeza que deseja remover o quarto ${roomNumber}?`)) {
            return;
        }

        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/${reservationId}/remove-room?roomNumber=${roomNumber}`,
                { method: 'PUT' }
            );

            if (response.ok) {
                this.showAlert('Quarto removido com sucesso!', 'success');
                await this.manageReservation(reservationId);
                await this.updateValueSection(reservationId);
                await this.loadReservations(); // ✅ Recarrega a lista para atualizar os valores
            } else {
                throw new Error('Erro ao remover quarto');
            }
        } catch (error) {
            console.error('Erro ao remover quarto:', error);
            this.showAlert('Erro ao remover quarto: ' + error.message, 'error');
        }
    }

    async addGuest(reservationId) {
        const guestName = document.getElementById('newGuestName').value.trim();
        
        if (!guestName) {
            this.showAlert('Por favor, digite o nome do hóspede', 'error');
            return;
        }

        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/add-guest/${reservationId}?nameGuest=${encodeURIComponent(guestName)}`,
                { method: 'PUT' }
            );

            if (response.ok) {
                this.showAlert('Hóspede adicionado com sucesso!', 'success');
                document.getElementById('newGuestName').value = '';
                
                await this.updateValueSection(reservationId);
                await this.manageReservation(reservationId);
                await this.loadReservations(); // ✅ Recarrega a lista para atualizar os valores
                
                console.log(`👥 Hóspede "${guestName}" adicionado - valor recalculado automaticamente`);
            } else {
                throw new Error('Erro ao adicionar hóspede');
            }
        } catch (error) {
            console.error('Erro ao adicionar hóspede:', error);
            this.showAlert('Erro ao adicionar hóspede', 'error');
        }
    }

    async removeGuest(reservationId, guestName) {
        if (!confirm(`Tem certeza que deseja remover o hóspede ${guestName}?`)) {
            return;
        }

        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/remove-guest/${reservationId}?nameGuest=${encodeURIComponent(guestName)}`,
                { method: 'PUT' }
            );

            if (response.ok) {
                this.showAlert('Hóspede removido com sucesso!', 'success');
                
                await this.updateValueSection(reservationId);
                await this.manageReservation(reservationId);
                await this.loadReservations(); // ✅ Recarrega a lista para atualizar os valores
                
                console.log(`👥 Hóspede "${guestName}" removido - valor recalculado automaticamente`);
            } else {
                throw new Error('Erro ao remover hóspede');
            }
        } catch (error) {
            console.error('Erro ao remover hóspede:', error);
            this.showAlert('Erro ao remover hóspede', 'error');
        }
    }

    async cancelReservation(reservationId) {
        if (!confirm('Tem certeza que deseja cancelar esta reserva?')) {
            return;
        }

        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/cancele/${reservationId}`,
                { method: 'PUT' }
            );

            if (response.ok) {
                this.showAlert('Reserva cancelada com sucesso!', 'success');
                this.closeManageModal();
                this.loadReservations();
            } else {
                throw new Error('Erro ao cancelar reserva');
            }
        } catch (error) {
            console.error('Erro ao cancelar reserva:', error);
            this.showAlert('Erro ao cancelar reserva', 'error');
        }
    }

    async deleteReservation(reservationId) {
        try {
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/find/${reservationId}`);
            if (!response.ok) {
                throw new Error('Não foi possível carregar os detalhes da reserva');
            }
            
            const reservation = await response.json();
            const guestNames = reservation.guest ? 
                (Array.isArray(reservation.guest) ? 
                    reservation.guest.map(g => g.name).join(', ') : 
                    Array.from(reservation.guest).map(g => g.name).join(', ')) : 
                'N/A';
            
            const confirmMessage = `
                Tem certeza que deseja EXCLUIR PERMANENTEMENTE esta reserva?
                
                📋 Detalhes da Reserva:
                • ID: #${reservation.id}
                • Hóspedes: ${guestNames}
                • Quartos: ${reservation.rooms ? reservation.rooms.map(r => r.number).join(', ') : 'N/A'}
                • Datas: ${reservation.reservedDays ? (Array.isArray(reservation.reservedDays) ? reservation.reservedDays.length : reservation.reservedDays.size) : 0} noites
                • Status: ${reservation.reserveStatus || 'N/A'}
                
                ⚠️ ESTA AÇÃO NÃO PODE SER DESFEITA!
            `.replace(/^ +/gm, '');

            if (!confirm(confirmMessage)) {
                return;
            }

            const deleteResponse = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/delete/${reservationId}`,
                { method: 'DELETE' }
            );

            if (deleteResponse.ok) {
                this.showAlert('Reserva excluída com sucesso!', 'success');
                this.closeManageModal();
                this.loadReservations();
            } else {
                const errorText = await deleteResponse.text();
                throw new Error(errorText || 'Erro ao excluir reserva');
            }
        } catch (error) {
            console.error('Erro ao excluir reserva:', error);
            this.showAlert('Erro ao excluir reserva: ' + error.message, 'error');
        }
    }

    async checkIn(reservationId) {
        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/check-in/${reservationId}`,
                { method: 'PUT' }
            );

            if (response.ok) {
                this.showAlert('Check-in realizado com sucesso!', 'success');
                this.closeManageModal();
                this.loadReservations();
            } else {
                throw new Error('Erro ao realizar check-in');
            }
        } catch (error) {
            console.error('Erro ao realizar check-in:', error);
            this.showAlert('Erro ao realizar check-in', 'error');
        }
    }

    async checkOut(reservationId) {
        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/check-out/${reservationId}`,
                { method: 'PUT' }
            );

            if (response.ok) {
                this.showAlert('Check-out realizado com sucesso!', 'success');
                this.closeManageModal();
                this.loadReservations();
            } else {
                throw new Error('Erro ao realizar check-out');
            }
        } catch (error) {
            console.error('Erro ao realizar check-out:', error);
            this.showAlert('Erro ao realizar check-out', 'error');
        }
    }

    viewFullDetails(reservationId) {
        this.viewReservationDetails(reservationId);
    }

    showAlert(message, type = 'info') {
        const alert = document.createElement('div');
        alert.className = `alert alert-${type}`;
        alert.textContent = message;

        const container = document.querySelector('.container');
        container.insertBefore(alert, container.firstChild);

        setTimeout(() => {
            alert.remove();
        }, 5000);
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Inicializar quando o DOM estiver carregado
let reservationManager;
document.addEventListener('DOMContentLoaded', () => {
    reservationManager = new ReservationManager();
    
    // ✅ Adicionar função global para debug
    window.debugReservation = function() {
        if (window.reservationManager) {
            window.reservationManager.quickDebug();
        } else {
            console.log('❌ ReservationManager não encontrado');
        }
    };
    
    console.log('🔧 ReservationManager carregado com sistema de cálculo de valores no frontend');
    console.log('💡 Use debugReservation() no console para testar');
});

// Fechar modais ao clicar fora
window.onclick = function(event) {
    const modals = document.getElementsByClassName('modal');
    for (let modal of modals) {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    }
}