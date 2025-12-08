class HotelSystem {
    constructor() {
        this.baseUrl = '';
        this.token = localStorage.getItem('jwtToken');
        this.initEventListeners();
        this.checkAuthentication();
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
        this.loadDashboardData();
    }

    async loadUserInfo() {
        try {
            const userInfoElement = document.getElementById('userInfo');
            const userEmail = localStorage.getItem('userInfo');
            
            if (userInfoElement && userEmail) {
                userInfoElement.textContent = `Usuário: ${userEmail}`;
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

    async loadDashboardData() {
        try {
            const [reservesRes, roomsRes] = await Promise.all([
                this.makeAuthenticatedRequest('/reserve/all'),
                this.makeAuthenticatedRequest('/room/all')
            ]);

            if(reservesRes.ok && roomsRes.ok) {
                const reserves = await reservesRes.json();
                const rooms = await roomsRes.json();

                this.updateStats(reserves, rooms);
                this.displayActiveReserves(reserves);
                this.displayAvailableRooms(rooms);
                this.displayUpcomingCheckins(reserves);
                this.displayTodayMovements(reserves);
            } else {
                throw new Error('Erro na resposta da API');
            }

        } catch (error) {
            console.error('Erro ao carregar dados:', error);
            this.showAlert('Erro ao carregar dados do dashboard', 'error');
        }
    }

    updateStats(reserves, rooms) {
        // Total de reservas ativas
        const activeReserves = reserves.filter(r => 
            r.reserveStatus !== 'CANCELLED' && 
            (!r.checkOut || r.checkOut.length === 0)
        );
        document.getElementById('totalReserves').textContent = activeReserves.length;

        // Quartos disponíveis
        const availableRooms = rooms.filter(room => room.roomStatus === 'VAGUE');
        document.getElementById('availableRoomsCount').textContent = availableRooms.length;

        // Quartos ocupados
        const occupiedRooms = rooms.filter(room => room.roomStatus === 'OCCUPIED');
        document.getElementById('occupiedRoomsCount').textContent = occupiedRooms.length;
    }

    displayActiveReserves(reserves) {
        const container = document.getElementById('activeReserves');
        if (!container) return;
        
        const activeReserves = reserves.filter(r => 
            r.reserveStatus !== 'CANCELLED' && 
            (!r.checkOut || r.checkOut.length === 0)
        );

        if (activeReserves.length === 0) {
            container.innerHTML = '<p>Nenhuma reserva ativa</p>';
            return;
        }

        const html = activeReserves.map(reserve => {
            const periodInfo = this.formatCheckInOutDates(reserve);
            return `
                <div class="today-movement-card">
                    <strong>Reserva #${reserve.id}</strong><br>
                    <strong>Hóspedes:</strong> ${
                        reserve.guest && reserve.guest.length > 0
                            ? reserve.guest.map(g => g.name || `ID: ${g.id}`).join(', ')
                            : 'N/A'
                    }<br>
                    <strong>Quarto:</strong> ${reserve.rooms && reserve.rooms.length > 0 ? reserve.rooms[0].number : 'N/A'}<br>
                    <strong>Período:</strong> ${periodInfo.period}<br>
                    <strong>Status:</strong> <span class="badge ${this.getReserveStatusClass(reserve)}">${this.getReserveStatusText(reserve)}</span>
                </div>
            `;
        }).join('');

        container.innerHTML = html;
    }

    displayAvailableRooms(rooms) {
        const container = document.getElementById('availableRooms');
        if (!container) return;
        
        const availableRooms = rooms.filter(room => room.roomStatus === 'VAGUE');

        if (availableRooms.length === 0) {
            container.innerHTML = '<p>Nenhum quarto disponível</p>';
            return;
        }

        const html = availableRooms.map(room => `
            <div class="today-movement-card">
                <strong>Quarto ${room.number}</strong><br>
                <span class="badge badge-${room.exclusiveRoom ? 'exclusive' : 'shared'}">
                    ${room.exclusiveRoom ? 'Exclusivo' : 'Compartilhado'}
                </span>
                ${room.price ? `<br><strong>Preço:</strong> R$ ${parseFloat(room.price).toFixed(2)}` : ''}
                ${room.capacity ? `<br><strong>Capacidade:</strong> ${room.capacity} pessoas` : ''}
            </div>
        `).join('');

        container.innerHTML = html;
    }

    displayUpcomingCheckins(reserves) {
        const container = document.getElementById('upcomingCheckins');
        if (!container) return;

        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const nextWeek = new Date();
        nextWeek.setDate(today.getDate() + 7);
        nextWeek.setHours(23, 59, 59, 999);

        const upcomingReserves = reserves.filter(reserve => {
            if (!reserve.reservedDays || reserve.reservedDays.length === 0) return false;
            if (reserve.reserveStatus === 'CANCELLED') return false;
            if (reserve.checkIn && reserve.checkIn.length > 0) return false; // Já fez check-in
            
            const periodInfo = this.formatCheckInOutDates(reserve);
            const checkInDate = this.parseDate(periodInfo.checkIn);
            
            return checkInDate >= today && checkInDate <= nextWeek;
        });

        if (upcomingReserves.length === 0) {
            container.innerHTML = '<p>Nenhum check-in previsto para os próximos 7 dias</p>';
            return;
        }

        const html = upcomingReserves.map(reserve => {
            const periodInfo = this.formatCheckInOutDates(reserve);
            return `
                <div class="today-movement-card">
                    <strong>Reserva #${reserve.id}</strong><br>
                    <strong>Hóspede:</strong> ${reserve.guest && reserve.guest.length > 0 ? reserve.guest[0].name : 'N/A'}<br>
                    <strong>Check-in:</strong> ${periodInfo.checkIn}<br>
                    <strong>Check-out:</strong> ${periodInfo.checkOut}<br>
                    <strong>Quarto:</strong> ${reserve.rooms && reserve.rooms.length > 0 ? reserve.rooms[0].number : 'N/A'}<br>
                    <strong>Noites:</strong> ${periodInfo.nights}
                </div>
            `;
        }).join('');

        container.innerHTML = html;
    }

    displayTodayMovements(reserves) {
        const checkinsContainer = document.getElementById('todayCheckins');
        const checkoutsContainer = document.getElementById('todayCheckouts');
        const todayDateElement = document.getElementById('todayDate');
        
        if (!checkinsContainer || !checkoutsContainer) return;

        // Formatar data de hoje
        const today = new Date();
        const todayFormatted = today.toISOString().split('T')[0];
        const todayFormattedBR = today.toLocaleDateString('pt-BR');
        
        if (todayDateElement) {
            todayDateElement.textContent = todayFormattedBR;
        }

        const todayCheckins = [];
        const todayCheckouts = [];

        reserves.forEach(reserve => {
            if (!reserve.reservedDays || reserve.reservedDays.length === 0) return;
            if (reserve.reserveStatus === 'CANCELLED') return;

            const periodInfo = this.formatCheckInOutDates(reserve);
            
            // Verificar check-ins de hoje (primeira data é hoje)
            if (periodInfo.checkInFormatted === todayFormatted) {
                todayCheckins.push({
                    reserve,
                    periodInfo
                });
            }

            // Verificar check-outs de hoje (última data é hoje)
            if (periodInfo.checkOutFormatted === todayFormatted) {
                todayCheckouts.push({
                    reserve,
                    periodInfo
                });
            }
        });

        // Exibir check-ins de hoje
        if (todayCheckins.length === 0) {
            checkinsContainer.innerHTML = '<p>Nenhum check-in para hoje</p>';
        } else {
            const checkinsHtml = todayCheckins.map(item => {
                const hasCheckedIn = item.reserve.checkIn && item.reserve.checkIn.length > 0;
                return `
                    <div class="today-movement-card today-checkin">
                        <strong>Reserva #${item.reserve.id}</strong><br>
                        <strong>Hóspede:</strong> ${item.reserve.guest && item.reserve.guest.length > 0 ? item.reserve.guest[0].name : 'N/A'}<br>
                        <strong>Quarto:</strong> ${item.reserve.rooms && item.reserve.rooms.length > 0 ? item.reserve.rooms[0].number : 'N/A'}<br>
                        <strong>Período:</strong> ${item.periodInfo.period}<br>
                        <strong>Noites:</strong> ${item.periodInfo.nights}<br>
                        <strong>Status:</strong> <span class="badge ${hasCheckedIn ? 'badge-checked-in' : 'badge-confirmed'}">
                            ${hasCheckedIn ? 'CHECK-IN REALIZADO' : 'CHECK-IN HOJE'}
                        </span>
                        ${!hasCheckedIn ? `
                            <div class="movement-actions">
                                <button class="btn btn-sm btn-success" onclick="hotelSystem.processCheckin(${item.reserve.id})">
                                    ✅ Confirmar Check-in
                                </button>
                            </div>
                        ` : ''}
                    </div>
                `;
            }).join('');
            checkinsContainer.innerHTML = checkinsHtml;
        }

        // Exibir check-outs de hoje
        if (todayCheckouts.length === 0) {
            checkoutsContainer.innerHTML = '<p>Nenhum check-out para hoje</p>';
        } else {
            const checkoutsHtml = todayCheckouts.map(item => {
                const hasCheckedOut = item.reserve.checkOut && item.reserve.checkOut.length > 0;
                return `
                    <div class="today-movement-card today-checkout">
                        <strong>Reserva #${item.reserve.id}</strong><br>
                        <strong>Hóspede:</strong> ${item.reserve.guest && item.reserve.guest.length > 0 ? item.reserve.guest[0].name : 'N/A'}<br>
                        <strong>Quarto:</strong> ${item.reserve.rooms && item.reserve.rooms.length > 0 ? item.reserve.rooms[0].number : 'N/A'}<br>
                        <strong>Período:</strong> ${item.periodInfo.period}<br>
                        <strong>Noites:</strong> ${item.periodInfo.nights}<br>
                        <strong>Status:</strong> <span class="badge ${hasCheckedOut ? 'badge-checked-out' : 'badge-busy'}">
                            ${hasCheckedOut ? 'CHECK-OUT REALIZADO' : 'CHECK-OUT HOJE'}
                        </span>
                        ${!hasCheckedOut ? `
                            <div class="movement-actions">
                                <button class="btn btn-sm btn-primary" onclick="hotelSystem.processCheckout(${item.reserve.id})">
                                    🚪 Processar Check-out
                                </button>
                            </div>
                        ` : ''}
                    </div>
                `;
            }).join('');
            checkoutsContainer.innerHTML = checkoutsHtml;
        }
    }

    formatCheckInOutDates(reservation) {
        if (!reservation.reservedDays || reservation.reservedDays.length === 0) {
            return { 
                checkIn: 'N/A', 
                checkOut: 'N/A', 
                period: 'N/A',
                nights: 0,
                checkInFormatted: null,
                checkOutFormatted: null
            };
        }

        try {
            const dates = Array.from(reservation.reservedDays)
                .map(date => this.parseDate(date))
                .filter(date => !isNaN(date))
                .sort((a, b) => a - b);

            if (dates.length === 0) {
                return { 
                    checkIn: 'N/A', 
                    checkOut: 'N/A', 
                    period: 'N/A',
                    nights: 0,
                    checkInFormatted: null,
                    checkOutFormatted: null
                };
            }

            const checkIn = dates[0];
            const checkOut = dates[dates.length - 1];
            
            return {
                checkIn: this.formatDate(checkIn),
                checkOut: this.formatDate(checkOut),
                period: `${this.formatDate(checkIn)} a ${this.formatDate(checkOut)}`,
                nights: dates.length,
                checkInFormatted: this.formatDateForInput(checkIn),
                checkOutFormatted: this.formatDateForInput(checkOut)
            };
        } catch (error) {
            console.error('Erro ao formatar datas de check-in/out:', error);
            return { 
                checkIn: 'Erro', 
                checkOut: 'Erro', 
                period: 'Erro',
                nights: 0,
                checkInFormatted: null,
                checkOutFormatted: null
            };
        }
    }

    parseDate(dateString) {
        if (typeof dateString === 'string') {
            if (/^\d{4}-\d{2}-\d{2}$/.test(dateString)) {
                const [year, month, day] = dateString.split('-').map(Number);
                return new Date(year, month - 1, day);
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
        
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        
        return `${year}-${month}-${day}`;
    }

    async processCheckin(reserveId) {
        try {
            const response = await this.makeAuthenticatedRequest(`/reserve/check-in/${reserveId}`, {
                method: 'PUT'
            });

            if (response.ok) {
                this.showAlert('Check-in realizado com sucesso!', 'success');
                this.loadDashboardData();
            } else {
                const errorText = await response.text();
                throw new Error(errorText || 'Erro ao processar check-in');
            }
        } catch (error) {
            console.error('Erro ao processar check-in:', error);
            this.showAlert('Erro ao processar check-in: ' + error.message, 'error');
        }
    }

    async processCheckout(reserveId) {
        try {
            const response = await this.makeAuthenticatedRequest(`/reserve/check-out/${reserveId}`, {
                method: 'PUT'
            });

            if (response.ok) {
                this.showAlert('Check-out realizado com sucesso!', 'success');
                this.loadDashboardData();
            } else {
                const errorText = await response.text();
                throw new Error(errorText || 'Erro ao processar check-out');
            }
        } catch (error) {
            console.error('Erro ao processar check-out:', error);
            this.showAlert('Erro ao processar check-out: ' + error.message, 'error');
        }
    }

    getReserveStatusClass(reserve) {
        if (reserve.reserveStatus === 'CANCELLED') {
            return 'badge-cancelled';
        }
        if (reserve.checkIn && reserve.checkIn.length > 0) {
            return 'badge-checked-in';
        }
        return 'badge-confirmed';
    }

    getReserveStatusText(reserve) {
        if (reserve.reserveStatus === 'CANCELLED') {
            return 'CANCELADA';
        }
        if (reserve.checkIn && reserve.checkIn.length > 0) {
            return 'CHECK-IN REALIZADO';
        }
        return 'CONFIRMADA';
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

    toggleExpand(containerId, button) {
        const container = document.getElementById(containerId);
        if (container.classList.contains('collapsed')) {
            container.classList.remove('collapsed');
            container.classList.add('expanded');
            button.textContent = 'Mostrar Menos';
        } else {
            container.classList.remove('expanded');
            container.classList.add('collapsed');
            button.textContent = 'Mostrar Todos';
        }
    }
}

// Inicializar quando o DOM estiver carregado
document.addEventListener('DOMContentLoaded', () => {
    window.hotelSystem = new HotelSystem();
});