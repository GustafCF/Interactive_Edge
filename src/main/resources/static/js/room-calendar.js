class RoomCalendar {
    constructor() {
        // this.API_BASE_URL = 'http://191.217.217.80:8080';
        this.API_BASE_URL = 'http://localhost:8080';
        this.currentDate = new Date();
        this.currentRoom = null;
        this.roomReservations = [];
        this.selectedDates = new Set();
        this.allReservations = [];
        this.selectedReservationId = null;
        this.init();
    }

    async init() {
        this.checkAuth();
        this.setupEventListeners();
        await this.loadRoomData();
        this.loadUserInfo();
        await this.testApiResponse();
    }

    getToken() {
        return localStorage.getItem('jwtToken');
    }

    checkAuth() {
        const token = this.getToken();
        if (!token) {
            window.location.href = '/login';
            return false;
        }
        
        const tokenExpiry = localStorage.getItem('tokenExpiry');
        if (tokenExpiry && Date.now() > parseInt(tokenExpiry)) {
            this.logout();
            return false;
        }
        
        return true;
    }

    logout() {
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('tokenExpiry');
        localStorage.removeItem('userInfo');
        window.location.href = '/login';
    }

    loadUserInfo() {
        try {
            const userInfoElement = document.getElementById('userInfo');
            const userEmail = localStorage.getItem('userInfo');
            const userName = localStorage.getItem('userName');
            
            if (userInfoElement) {
                const displayName = userName || userEmail || 'Usuário';
                userInfoElement.innerHTML = `
                    <i class="fas fa-user"></i> ${displayName}
                `;
            }
        } catch (error) {
            console.error('Erro ao carregar informações do usuário:', error);
        }
    }

    showAlert(message, type = 'success') {
        const alertContainer = document.getElementById('alertContainer');
        
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
            warning: 'exclamation-triangle',
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
        
        if (type === 'success' || type === 'info') {
            setTimeout(() => {
                if (alert.parentNode) {
                    alert.style.animation = 'slideOut 0.3s ease';
                    setTimeout(() => alert.remove(), 300);
                }
            }, 5000);
        }
    }

    async apiRequest(url, options = {}) {
        const token = this.getToken();
        
        const defaultOptions = {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                ...options.headers
            }
        };

        const mergedOptions = { ...defaultOptions, ...options };

        try {
            const response = await fetch(url, mergedOptions);
            
            if (response.status === 401 || response.status === 403) {
                this.logout();
                return null;
            }
            
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('API request failed:', error);
            this.showAlert(`Erro na comunicação: ${error.message}`, 'error');
            return null;
        }
    }

    async loadRoomData() {
        if (!this.checkAuth()) return;

        const savedRoom = localStorage.getItem('selectedRoom');
        if (!savedRoom) {
            this.showAlert('Nenhum quarto selecionado', 'error');
            setTimeout(() => window.location.href = '/room', 2000);
            return;
        }

        try {
            this.currentRoom = JSON.parse(savedRoom);
            console.log('Quarto carregado do localStorage:', this.currentRoom);
            
            document.getElementById('roomNumberDisplay').innerHTML = `
                <i class="fas fa-door-closed"></i> Quarto #${this.currentRoom.number}
            `;

            const room = await this.apiRequest(`${this.API_BASE_URL}/room/find/${this.currentRoom.id}`);
            if (room) {
                console.log('Detalhes do quarto da API:', room);
                document.getElementById('roomTypeDisplay').innerHTML = `
                    <i class="fas ${room.exclusiveRoom ? 'fa-crown' : 'fa-users'}"></i>
                    ${room.exclusiveRoom ? 'Quarto Exclusivo' : 'Quarto Compartilhado'}
                `;
            } else {
                document.getElementById('roomTypeDisplay').innerHTML = `
                    <i class="fas fa-question-circle"></i> Tipo não disponível
                `;
            }

            await this.loadReservations();
        } catch (error) {
            console.error('Erro ao carregar dados do quarto:', error);
            this.showAlert('Erro ao carregar informações do quarto', 'error');
        }
    }

    async testApiResponse() {
        try {
            console.log('=== TESTE DA API ===');
            
            // Teste 1: Verificar endpoint de reservas
            const reserves = await this.apiRequest(`${this.API_BASE_URL}/reserve/all`);
            console.log('API /reserve/all response:', reserves);
            
            if (reserves && reserves.length > 0) {
                const firstReserve = reserves[0];
                console.log('Primeira reserva detalhada:', firstReserve);
                console.log('Estrutura dos quartos na primeira reserva:', firstReserve.rooms);
                console.log('Tipo de rooms:', typeof firstReserve.rooms);
                console.log('É array?', Array.isArray(firstReserve.rooms));
                
                if (firstReserve.rooms) {
                    const roomsArray = Array.isArray(firstReserve.rooms) ? firstReserve.rooms : Array.from(firstReserve.rooms || []);
                    console.log('Quartos como array:', roomsArray);
                    
                    // Verificar estrutura de cada quarto
                    roomsArray.forEach((room, index) => {
                        console.log(`Quarto ${index}:`, {
                            id: room.id,
                            number: room.number,
                            roomType: room.roomType,
                            hasNumber: !!room.number,
                            numberType: typeof room.number
                        });
                    });
                }
            }
            
            // Teste 2: Verificar endpoint do quarto
            if (this.currentRoom) {
                const room = await this.apiRequest(`${this.API_BASE_URL}/room/find/${this.currentRoom.id}`);
                console.log('API /room/find response:', room);
            }
            
        } catch (error) {
            console.error('Erro no teste da API:', error);
        }
    }

    async loadReservations() {
        const calendarLoading = document.getElementById('calendarLoading');

        calendarLoading.style.display = 'flex';

        try {
            // Carregar todas as reservas
            this.allReservations = await this.apiRequest(`${this.API_BASE_URL}/reserve/all`) || [];
            
            console.log('=== RESERVAS CARREGADAS DA API ===');
            console.log('Total de reservas:', this.allReservations.length);
            console.log('Reservas completas:', this.allReservations);
            
            // Converter currentRoom para tipos consistentes
            const currentRoomId = Number(this.currentRoom.id);
            const currentRoomNumber = String(this.currentRoom.number); // Converter para string
            
            console.log('Parâmetros para filtro:', {
                currentRoomId,
                currentRoomNumber,
                currentRoomIdType: typeof currentRoomId,
                currentRoomNumberType: typeof currentRoomNumber,
                currentRoomOriginal: this.currentRoom
            });
            
            // Filtrar reservas para o quarto específico - VERSÃO ROBUSTA
            this.roomReservations = this.allReservations.filter(reserve => {
                // Ignorar reservas canceladas
                if (reserve.reserveStatus === 'CANCELLED') {
                    console.log(`Reserva ${reserve.id} ignorada (CANCELLED)`);
                    return false;
                }
                
                // Verificar se a reserva tem quartos
                if (!reserve.rooms) {
                    console.log(`Reserva ${reserve.id} não tem quartos`);
                    return false;
                }
                
                // Converter Set para Array se necessário
                let roomsArray = [];
                if (Array.isArray(reserve.rooms)) {
                    roomsArray = reserve.rooms;
                } else if (reserve.rooms instanceof Set) {
                    roomsArray = Array.from(reserve.rooms);
                } else if (typeof reserve.rooms === 'object' && reserve.rooms !== null) {
                    // Pode ser um objeto ou Map
                    if (reserve.rooms[Symbol.iterator]) {
                        roomsArray = Array.from(reserve.rooms);
                    } else {
                        roomsArray = Object.values(reserve.rooms);
                    }
                }
                
                console.log(`Reserva ${reserve.id} - Quartos encontrados:`, roomsArray);
                
                // Verificar se algum quarto tem o número ou ID igual ao quarto atual
                const hasThisRoom = roomsArray.some(room => {
                    if (!room || typeof room !== 'object') {
                        console.log(`Reserva ${reserve.id} - Quarto inválido:`, room);
                        return false;
                    }
                    
                    // Tentar obter número do quarto de várias formas
                    const roomId = room.id;
                    const roomNumber = room.number || room.roomNumber || room.num;
                    
                    // Log para debug
                    console.log(`Reserva ${reserve.id} - Comparando quarto:`, {
                        roomId,
                        roomNumber,
                        roomIdType: typeof roomId,
                        roomNumberType: typeof roomNumber,
                        currentRoomId,
                        currentRoomNumber
                    });
                    
                    // Comparar tanto por ID quanto por número (ambos como string)
                    const roomIdMatch = String(roomId) === String(currentRoomId);
                    const roomNumberMatch = String(roomNumber) === currentRoomNumber;
                    
                    const match = roomIdMatch || roomNumberMatch;
                    
                    if (match) {
                        console.log(`✅ Reserva ${reserve.id} - ENCONTRADA CORRESPONDÊNCIA!`);
                        console.log(`   Quarto da reserva: ID=${roomId}, Número=${roomNumber}`);
                        console.log(`   Quarto atual: ID=${currentRoomId}, Número=${currentRoomNumber}`);
                    }
                    
                    return match;
                });
                
                console.log(`Reserva ${reserve.id} pertence ao quarto ${this.currentRoom.number}? ${hasThisRoom}`);
                return hasThisRoom;
            });
            
            console.log(`=== RESULTADO DO FILTRO ===`);
            console.log(`Reservas filtradas para o quarto ${this.currentRoom.number}:`, this.roomReservations.length);
            console.log('Reservas detalhadas:', this.roomReservations);
            
            this.debugRoomReservations(); // Debug detalhado das reservas
            
            this.renderCalendar();
        } catch (error) {
            console.error('Error loading reservations:', error);
            this.showAlert('Erro ao carregar reservas', 'error');
        }

        calendarLoading.style.display = 'none';
    }

    debugRoomReservations() {
        console.log('=== DEBUG DETALHADO DAS RESERVAS DO QUARTO ===');
        console.log('Quarto atual:', this.currentRoom);
        console.log(`Total de reservas carregadas: ${this.allReservations.length}`);
        console.log(`Reservas filtradas para quarto ${this.currentRoom.number}: ${this.roomReservations.length}`);
        
        if (this.allReservations.length > 0) {
            console.log('\n=== DETALHES DE TODAS AS RESERVAS ===');
            this.allReservations.forEach((reserve, index) => {
                console.log(`\nReserva ${index + 1} - ID: ${reserve.id}`);
                console.log('Status:', reserve.reserveStatus);
                
                // Mostrar quartos
                if (reserve.rooms) {
                    let roomsArray = [];
                    if (Array.isArray(reserve.rooms)) {
                        roomsArray = reserve.rooms;
                    } else if (reserve.rooms instanceof Set) {
                        roomsArray = Array.from(reserve.rooms);
                    } else if (typeof reserve.rooms === 'object') {
                        roomsArray = Object.values(reserve.rooms);
                    }
                    
                    console.log('Quartos encontrados:', roomsArray.length);
                    roomsArray.forEach((room, roomIndex) => {
                        if (room && typeof room === 'object') {
                            console.log(`  Quarto ${roomIndex + 1}:`, {
                                id: room.id,
                                number: room.number,
                                roomType: room.roomType,
                                hasNumber: !!room.number,
                                numberType: typeof room.number
                            });
                        } else {
                            console.log(`  Quarto ${roomIndex + 1}:`, room);
                        }
                    });
                } else {
                    console.log('Quartos: null ou undefined');
                }
                
                // Mostrar hóspedes
                if (reserve.guest) {
                    let guestsArray = [];
                    if (Array.isArray(reserve.guest)) {
                        guestsArray = reserve.guest;
                    } else if (reserve.guest instanceof Set) {
                        guestsArray = Array.from(reserve.guest);
                    }
                    console.log('Hóspedes:', guestsArray.map(g => g ? g.name : 'null'));
                }
                
                // Mostrar datas
                if (reserve.reservedDays) {
                    let datesArray = [];
                    if (Array.isArray(reserve.reservedDays)) {
                        datesArray = reserve.reservedDays;
                    } else if (reserve.reservedDays instanceof Set) {
                        datesArray = Array.from(reserve.reservedDays);
                    }
                    console.log('Datas reservadas:', datesArray.sort());
                }
            });
        }
        
        if (this.roomReservations.length > 0) {
            console.log('\n=== RESERVAS FILTRADAS DETALHADAS ===');
            this.roomReservations.forEach((reserve, index) => {
                console.log(`\nReserva filtrada ${index + 1} - ID: ${reserve.id}`);
                console.log('Status:', reserve.reserveStatus);
                
                if (reserve.reservedDays) {
                    let datesArray = [];
                    if (Array.isArray(reserve.reservedDays)) {
                        datesArray = reserve.reservedDays;
                    } else if (reserve.reservedDays instanceof Set) {
                        datesArray = Array.from(reserve.reservedDays);
                    }
                    console.log('Datas:', datesArray);
                }
            });
        }
    }

    renderCalendar() {
        const year = this.currentDate.getFullYear();
        const month = this.currentDate.getMonth();
        
        const monthNames = [
            'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
            'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
        ];
        document.getElementById('currentMonthDisplay').innerHTML = `
            <i class="fas fa-calendar"></i> ${monthNames[month]} ${year}
        `;

        const calendarGrid = document.getElementById('calendarGrid');
        while (calendarGrid.children.length > 7) {
            calendarGrid.removeChild(calendarGrid.lastChild);
        }

        const firstDay = new Date(year, month, 1);
        const lastDay = new Date(year, month + 1, 0);
        const firstDayOfWeek = firstDay.getDay();

        for (let i = 0; i < firstDayOfWeek; i++) {
            const emptyDay = document.createElement('div');
            emptyDay.className = 'calendar-day other-month';
            calendarGrid.appendChild(emptyDay);
        }

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        console.log(`=== RENDERIZANDO CALENDÁRIO ===`);
        console.log('Mês:', monthNames[month], year);
        console.log('Reservas para renderizar:', this.roomReservations.length);

        for (let day = 1; day <= lastDay.getDate(); day++) {
            const dayDate = new Date(year, month, day);
            const dayElement = document.createElement('div');
            dayElement.className = 'calendar-day';
            
            if (dayDate.getTime() === today.getTime()) {
                dayElement.classList.add('today');
            }

            if (dayDate < today) {
                dayElement.classList.add('past');
            }

            const reservationForDay = this.getReservationForDate(dayDate);
            const dateString = this.formatDate(dayDate);
            const isSelected = this.selectedDates.has(dateString);

            // Aplicar classes CSS
            if (reservationForDay) {
                dayElement.classList.add('reserved');
                console.log(`Dia ${dateString}: RESERVADO - ${reservationForDay.guestNames.join(', ')}`);
            } else if (!dayElement.classList.contains('past')) {
                dayElement.classList.add('available');
            }

            if (isSelected) {
                dayElement.classList.add('selected');
            }

            const dayNumber = document.createElement('div');
            dayNumber.className = 'day-number';
            dayNumber.textContent = day;
            dayElement.appendChild(dayNumber);

            if (reservationForDay) {
                const reservationInfo = document.createElement('div');
                reservationInfo.className = 'reservation-info';
                
                // Mostrar nomes dos hóspedes (até 2)
                reservationForDay.guestNames.slice(0, 2).forEach(guestName => {
                    const guestElement = document.createElement('div');
                    guestElement.className = 'guest-name';
                    guestElement.textContent = guestName.length > 10 ? 
                        guestName.substring(0, 8) + '...' : guestName;
                    guestElement.title = guestName;
                    reservationInfo.appendChild(guestElement);
                });

                // Indicador de mais hóspedes
                if (reservationForDay.guestNames.length > 2) {
                    const moreElement = document.createElement('div');
                    moreElement.className = 'more-guests';
                    moreElement.textContent = `+${reservationForDay.guestNames.length - 2}`;
                    moreElement.title = `Mais ${reservationForDay.guestNames.length - 2} hóspedes`;
                    reservationInfo.appendChild(moreElement);
                }

                // Status da reserva
                const status = document.createElement('div');
                status.className = `reservation-status status-${reservationForDay.status.toLowerCase()}`;
                
                const statusText = {
                    'CONFIRMED': 'Confirmada',
                    'CHECKED_IN': 'Check-in',
                    'CHECKED_OUT': 'Check-out',
                    'CANCELLED': 'Cancelada'
                }[reservationForDay.status] || reservationForDay.status;
                
                status.textContent = statusText;
                reservationInfo.appendChild(status);

                dayElement.appendChild(reservationInfo);
                
                // Adicionar evento para ver detalhes
                dayElement.addEventListener('click', (e) => {
                    e.stopPropagation();
                    if (reservationForDay.reservationId) {
                        this.viewReservationDetails(reservationForDay.reservationId);
                    }
                });
                
                dayElement.style.cursor = 'pointer';
                dayElement.title = `Clique para ver detalhes da reserva`;
            } else if (dayElement.classList.contains('available') || isSelected) {
                dayElement.style.cursor = 'pointer';
                dayElement.addEventListener('click', () => this.toggleDateSelection(dayDate));
            } else {
                dayElement.style.cursor = 'default';
            }

            calendarGrid.appendChild(dayElement);
        }
    }

    getReservationForDate(date) {
        const dateString = this.formatDate(date);
        
        // Procurar reservas que incluam esta data
        for (const reservation of this.roomReservations) {
            // Verificar se a reserva tem dias reservados
            if (!reservation.reservedDays) {
                continue;
            }
            
            // Converter Set para Array se necessário
            let datesArray = [];
            if (Array.isArray(reservation.reservedDays)) {
                datesArray = reservation.reservedDays;
            } else if (reservation.reservedDays instanceof Set) {
                datesArray = Array.from(reservation.reservedDays);
            }
            
            // Verificar se a data está na lista de dias reservados
            const isReserved = datesArray.some(reservedDate => {
                // Comparar strings de data (YYYY-MM-DD)
                const reservedDateStr = reservedDate.substring(0, 10);
                return reservedDateStr === dateString;
            });
            
            if (isReserved) {
                // Coletar nomes dos hóspedes
                const guestNames = [];
                
                if (reservation.guest) {
                    // Converter Set para Array se necessário
                    let guestsArray = [];
                    if (Array.isArray(reservation.guest)) {
                        guestsArray = reservation.guest;
                    } else if (reservation.guest instanceof Set) {
                        guestsArray = Array.from(reservation.guest);
                    }
                    
                    guestsArray.forEach(guest => {
                        if (guest && guest.name) {
                            guestNames.push(guest.name);
                        }
                    });
                }
                
                // Se não encontrar nomes, usar um padrão
                if (guestNames.length === 0) {
                    guestNames.push(`Reserva #${reservation.id}`);
                }
                
                return {
                    guestNames: guestNames,
                    status: reservation.reserveStatus || 'CONFIRMED',
                    reservationId: reservation.id,
                    reservation: reservation
                };
            }
        }
        
        return null;
    }

    toggleDateSelection(date) {
        const dateString = this.formatDate(date);
        
        if (this.selectedDates.has(dateString)) {
            this.selectedDates.delete(dateString);
        } else {
            const reservationForDay = this.getReservationForDate(date);
            if (reservationForDay) {
                this.showAlert('Esta data já está reservada', 'error');
                return;
            }
            this.selectedDates.add(dateString);
        }
        
        this.updateSelectedDatesDisplay();
        this.renderCalendar();
    }

    updateSelectedDatesDisplay() {
        const selectedDatesContainer = document.getElementById('selectedDates');
        
        if (this.selectedDates.size === 0) {
            selectedDatesContainer.innerHTML = `
                <p class="no-dates">
                    <i class="fas fa-calendar-plus"></i> Nenhuma data selecionada
                </p>
            `;
            return;
        }
        
        const sortedDates = Array.from(this.selectedDates).sort();
        selectedDatesContainer.innerHTML = '';
        
        sortedDates.forEach(dateString => {
            const dateTag = document.createElement('div');
            dateTag.className = 'date-tag';
            
            const date = this.parseDate(dateString);
            const formattedDate = date.toLocaleDateString('pt-BR', {
                weekday: 'short',
                day: '2-digit',
                month: '2-digit'
            });
            
            dateTag.innerHTML = `
                <span class="date-text">
                    <i class="fas fa-calendar-day"></i> ${formattedDate}
                </span>
                <button type="button" class="date-remove" data-date="${dateString}">
                    <i class="fas fa-times"></i>
                </button>
            `;
            
            const removeButton = dateTag.querySelector('.date-remove');
            removeButton.addEventListener('click', (e) => {
                e.stopPropagation();
                this.selectedDates.delete(dateString);
                this.updateSelectedDatesDisplay();
                this.renderCalendar();
            });
            
            selectedDatesContainer.appendChild(dateTag);
        });
    }

    formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    parseDate(dateString) {
        const parts = dateString.split('-');
        return new Date(parts[0], parts[1] - 1, parts[2]);
    }

    renderReservationsList() {
        const reservationsList = document.getElementById('reservationsList');
        reservationsList.innerHTML = '';

        if (this.roomReservations.length === 0) {
            reservationsList.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-calendar-times"></i>
                    <p>Nenhuma reserva encontrada para este quarto.</p>
                    <button class="btn btn-primary" onclick="roomCalendar.openReservationForm()">
                        <i class="fas fa-plus"></i> Criar Primeira Reserva
                    </button>
                </div>
            `;
            return;
        }

        const sortedReservations = [...this.roomReservations].sort((a, b) => {
            let datesA = [];
            if (Array.isArray(a.reservedDays)) {
                datesA = a.reservedDays;
            } else if (a.reservedDays instanceof Set) {
                datesA = Array.from(a.reservedDays);
            }
            
            let datesB = [];
            if (Array.isArray(b.reservedDays)) {
                datesB = b.reservedDays;
            } else if (b.reservedDays instanceof Set) {
                datesB = Array.from(b.reservedDays);
            }
            
            const dateA = datesA[0] ? new Date(datesA[0]) : new Date(0);
            const dateB = datesB[0] ? new Date(datesB[0]) : new Date(0);
            return dateB - dateA;
        });

        sortedReservations.forEach(reservation => {
            const reservationItem = document.createElement('div');
            reservationItem.className = 'reservation-item';
            reservationItem.dataset.id = reservation.id;

            let datesArray = [];
            if (Array.isArray(reservation.reservedDays)) {
                datesArray = reservation.reservedDays;
            } else if (reservation.reservedDays instanceof Set) {
                datesArray = Array.from(reservation.reservedDays);
            }
            
            const dates = datesArray.length > 0 
                ? datesArray.map(date => {
                    const dateStr = date.substring(0, 10);
                    const [year, month, day] = dateStr.split('-');
                    return `${day}/${month}/${year}`;
                }).join(', ') 
                : 'Datas não disponíveis';

            const statusText = {
                'CONFIRMED': 'Confirmada',
                'CHECKED_IN': 'Check-in Realizado',
                'CHECKED_OUT': 'Check-out Realizado',
                'CANCELLED': 'Cancelada'
            }[reservation.reserveStatus] || 'Desconhecido';

            const statusClass = `status-${reservation.reserveStatus ? reservation.reserveStatus.toLowerCase() : 'unknown'}`;

            let guestsHtml = '';
            if (reservation.guest) {
                let guestsArray = [];
                if (Array.isArray(reservation.guest)) {
                    guestsArray = reservation.guest;
                } else if (reservation.guest instanceof Set) {
                    guestsArray = Array.from(reservation.guest);
                }
                
                if (guestsArray.length > 0) {
                    guestsHtml = `
                        <div class="reservation-guests">
                            <strong><i class="fas fa-users"></i> Hóspedes (${guestsArray.length}):</strong>
                            <div class="guest-list">
                                ${guestsArray.slice(0, 3).map(guest => `
                                    <div class="guest-item">
                                        <span class="guest-name">${guest.name || `Hóspede ${guest.id}`}</span>
                                        ${guest.email ? `<small class="guest-email">${guest.email}</small>` : ''}
                                        ${guest.phone ? `<small class="guest-phone">${guest.phone}</small>` : ''}
                                    </div>
                                `).join('')}
                                ${guestsArray.length > 3 ? 
                                    `<div class="more-guests">+${guestsArray.length - 3} mais</div>` : ''
                                }
                            </div>
                        </div>
                    `;
                } else {
                    guestsHtml = '<div class="reservation-guests"><strong>Hóspedes:</strong> Nenhum hóspede associado</div>';
                }
            } else {
                guestsHtml = '<div class="reservation-guests"><strong>Hóspedes:</strong> Nenhum hóspede associado</div>';
            }

            reservationItem.innerHTML = `
                <div class="reservation-header">
                    <div class="reservation-id">
                        <strong><i class="fas fa-hashtag"></i> Reserva #${reservation.id}</strong>
                    </div>
                    <div class="reservation-status ${statusClass}">
                        <i class="fas fa-circle"></i> ${statusText}
                    </div>
                </div>
                <div class="reservation-dates">
                    <strong><i class="fas fa-calendar-alt"></i> Datas:</strong> ${dates}
                </div>
                ${guestsHtml}
                ${reservation.checkIn && reservation.checkIn.length > 0 ? 
                    `<div class="reservation-checkin">
                        <strong><i class="fas fa-sign-in-alt"></i> Check-in:</strong> 
                        ${new Date(reservation.checkIn[0]).toLocaleString('pt-BR')}
                    </div>` : ''
                }
                ${reservation.checkOut && reservation.checkOut.length > 0 ? 
                    `<div class="reservation-checkout">
                        <strong><i class="fas fa-sign-out-alt"></i> Check-out:</strong> 
                        ${new Date(reservation.checkOut[0]).toLocaleString('pt-BR')}
                    </div>` : ''
                }
                <div class="reservation-actions">
                    <button class="btn btn-info btn-sm" onclick="roomCalendar.manageReservation(${reservation.id})">
                        <i class="fas fa-cog"></i> Gerenciar
                    </button>
                    <button class="btn btn-primary btn-sm" onclick="roomCalendar.viewReservationDetails(${reservation.id})">
                        <i class="fas fa-eye"></i> Detalhes
                    </button>
                    ${reservation.reserveStatus === 'CONFIRMED' ? `
                        <button class="btn btn-success btn-sm" onclick="roomCalendar.performCheckIn(${reservation.id})">
                            <i class="fas fa-check"></i> Check-in
                        </button>
                        <button class="btn btn-danger btn-sm" onclick="roomCalendar.showCancelConfirmation(${reservation.id})">
                            <i class="fas fa-ban"></i> Cancelar
                        </button>
                    ` : ''}
                    ${reservation.reserveStatus === 'CHECKED_IN' ? `
                        <button class="btn btn-primary btn-sm" onclick="roomCalendar.performCheckOut(${reservation.id})">
                            <i class="fas fa-flag-checkered"></i> Check-out
                        </button>
                    ` : ''}
                </div>
            `;

            reservationsList.appendChild(reservationItem);
        });
    }

    openReservationForm() {
        this.selectedDates.clear();
        this.updateSelectedDatesDisplay();
        document.getElementById('reservationForm').style.display = 'block';
        this.renderCalendar();
        
        document.getElementById('reservationForm').scrollIntoView({ 
            behavior: 'smooth', 
            block: 'start' 
        });
    }

    closeReservationForm() {
        document.getElementById('reservationForm').style.display = 'none';
        document.getElementById('reserveForm').reset();
        this.selectedDates.clear();
        this.updateSelectedDatesDisplay();
        this.renderCalendar();
    }

    validateReservationForm() {
        const guestName = document.getElementById('guestName').value.trim();
        const guestNameError = document.getElementById('guestNameError');
        
        guestNameError.textContent = '';
        
        let isValid = true;
        
        if (this.selectedDates.size === 0) {
            this.showAlert('Selecione pelo menos uma data para reservar', 'error');
            isValid = false;
        }
        
        if (!guestName) {
            guestNameError.textContent = 'Nome do hóspede é obrigatório';
            isValid = false;
        } else if (guestName.length < 3) {
            guestNameError.textContent = 'Nome deve ter pelo menos 3 caracteres';
            isValid = false;
        }
        
        return isValid;
    }

    async createReservation(event) {
        event.preventDefault();
        
        if (!this.validateReservationForm()) {
            return;
        }

        const guestName = document.getElementById('guestName').value.trim();
        const reserveBtn = document.getElementById('reserveBtn');
        
        reserveBtn.disabled = true;
        reserveBtn.classList.add('loading');
        
        try {
            const datesArray = Array.from(this.selectedDates).map(dateStr => {
                const date = this.parseDate(dateStr);
                return this.formatDate(date);
            });
            
            const reservationData = {
                dates: datesArray,
                guestName: guestName,
                roomNumber: this.currentRoom.number
            };
            
            console.log('📤 ENVIANDO DADOS PARA CRIAR RESERVA:');
            console.log('Dados:', reservationData);
            console.log('Endpoint:', `${this.API_BASE_URL}/reserve/insert`);
            
            const response = await this.apiRequest(`${this.API_BASE_URL}/reserve/insert`, {
                method: 'POST',
                body: JSON.stringify(reservationData)
            });
            
            console.log('📥 RESPOSTA DA API:', response);
            
            if (response) {
                this.showAlert('Reserva criada com sucesso!', 'success');
                this.closeReservationForm();
                
                // Recarregar dados com delay para dar tempo para o backend processar
                setTimeout(() => {
                    console.log('Recarregando reservas após criação...');
                    this.loadReservations();
                }, 1000);
            } else {
                throw new Error('Erro ao criar reserva - resposta vazia');
            }
        } catch (error) {
            console.error('Erro ao criar reserva:', error);
            this.showAlert(`Erro ao criar reserva: ${error.message}`, 'error');
        } finally {
            reserveBtn.disabled = false;
            reserveBtn.classList.remove('loading');
        }
    }

    async manageReservation(reservationId) {
        try {
            const response = await this.apiRequest(`${this.API_BASE_URL}/reserve/find/${reservationId}`);
            
            if (response) {
                this.openManageModal(response);
            } else {
                throw new Error('Erro ao carregar reserva');
            }
        } catch (error) {
            console.error('Erro ao carregar reserva:', error);
            this.showAlert('Erro ao carregar reserva', 'error');
        }
    }

    async viewReservationDetails(reservationId) {
        try {
            const response = await this.apiRequest(`${this.API_BASE_URL}/reserve/find/${reservationId}`);
            
            if (response) {
                this.openDetailsModal(response);
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
        
        let datesArray = [];
        if (Array.isArray(reservation.reservedDays)) {
            datesArray = reservation.reservedDays;
        } else if (reservation.reservedDays instanceof Set) {
            datesArray = Array.from(reservation.reservedDays);
        }
        
        let guestsArray = [];
        if (Array.isArray(reservation.guest)) {
            guestsArray = reservation.guest;
        } else if (reservation.guest instanceof Set) {
            guestsArray = Array.from(reservation.guest);
        }
        
        let roomsArray = [];
        if (Array.isArray(reservation.rooms)) {
            roomsArray = reservation.rooms;
        } else if (reservation.rooms instanceof Set) {
            roomsArray = Array.from(reservation.rooms);
        }
        
        const html = `
            <div class="management-section">
                <h4 class="section-title"><i class="fas fa-info-circle"></i> Informações da Reserva</h4>
                <div class="info-grid">
                    <div class="info-item">
                        <strong>ID:</strong> ${reservation.id}
                    </div>
                    <div class="info-item">
                        <strong>Status:</strong> ${this.getStatusText(reservation.reserveStatus)}
                    </div>
                    <div class="info-item">
                        <strong>Hóspede Principal:</strong> ${this.getGuestName(reservation)}
                    </div>
                    <div class="info-item">
                        <strong>Quarto:</strong> ${this.getRoomNumber(reservation)}
                    </div>
                </div>
            </div>

            <div class="management-section">
                <h4 class="section-title"><i class="fas fa-calendar-alt"></i> Datas da Reserva</h4>
                <div class="current-dates">
                    ${this.formatCurrentDates(datesArray)}
                </div>
            </div>

            <div class="management-section">
                <h4 class="section-title"><i class="fas fa-users"></i> Hóspedes</h4>
                <div class="guest-list">
                    ${this.formatGuestList(guestsArray)}
                </div>
            </div>

            <div class="management-actions">
                ${reservation.reserveStatus === 'CONFIRMED' ? `
                    <button class="btn btn-success" onclick="roomCalendar.performCheckIn(${reservation.id})">
                        <i class="fas fa-check"></i> Realizar Check-in
                    </button>
                    <button class="btn btn-danger" onclick="roomCalendar.showCancelConfirmation(${reservation.id})">
                        <i class="fas fa-ban"></i> Cancelar Reserva
                    </button>
                ` : ''}
                ${reservation.reserveStatus === 'CHECKED_IN' ? `
                    <button class="btn btn-primary" onclick="roomCalendar.performCheckOut(${reservation.id})">
                        <i class="fas fa-flag-checkered"></i> Realizar Check-out
                    </button>
                ` : ''}
                <button class="btn btn-secondary" onclick="roomCalendar.closeModal('manageModal')">
                    <i class="fas fa-times"></i> Fechar
                </button>
            </div>
        `;

        container.innerHTML = html;
        this.openModal('manageModal');
    }

    openDetailsModal(reservation) {
        const container = document.getElementById('reservationDetailsContent');
        
        let datesArray = [];
        if (Array.isArray(reservation.reservedDays)) {
            datesArray = reservation.reservedDays;
        } else if (reservation.reservedDays instanceof Set) {
            datesArray = Array.from(reservation.reservedDays);
        }
        
        let guestsArray = [];
        if (Array.isArray(reservation.guest)) {
            guestsArray = reservation.guest;
        } else if (reservation.guest instanceof Set) {
            guestsArray = Array.from(reservation.guest);
        }
        
        let roomsArray = [];
        if (Array.isArray(reservation.rooms)) {
            roomsArray = reservation.rooms;
        } else if (reservation.rooms instanceof Set) {
            roomsArray = Array.from(reservation.rooms);
        }
        
        const html = `
            <div class="details-section">
                <h4><i class="fas fa-info-circle"></i> Informações da Reserva</h4>
                <table class="details-table">
                    <tr>
                        <th>ID:</th>
                        <td>${reservation.id}</td>
                    </tr>
                    <tr>
                        <th>Status:</th>
                        <td><span class="status-badge status-${reservation.reserveStatus?.toLowerCase() || 'unknown'}">${this.getStatusText(reservation.reserveStatus)}</span></td>
                    </tr>
                    <tr>
                        <th>Criada em:</th>
                        <td>${reservation.createdAt ? new Date(reservation.createdAt).toLocaleString('pt-BR') : 'N/A'}</td>
                    </tr>
                    <tr>
                        <th>Atualizada em:</th>
                        <td>${reservation.updatedAt ? new Date(reservation.updatedAt).toLocaleString('pt-BR') : 'N/A'}</td>
                    </tr>
                </table>
            </div>

            <div class="details-section">
                <h4><i class="fas fa-calendar-alt"></i> Datas</h4>
                <div class="dates-list">
                    ${this.formatCurrentDates(datesArray)}
                </div>
            </div>

            <div class="details-section">
                <h4><i class="fas fa-bed"></i> Quarto</h4>
                <div class="room-info-card">
                    <i class="fas fa-door-closed"></i>
                    <div>
                        <strong>Quarto #${this.getRoomNumber(reservation)}</strong>
                        <br>
                        <small>${roomsArray[0]?.exclusiveRoom ? 'Exclusivo' : 'Compartilhado'}</small>
                    </div>
                </div>
            </div>

            <div class="details-section">
                <h4><i class="fas fa-users"></i> Hóspedes</h4>
                <div class="guests-details">
                    ${this.formatGuestDetails(guestsArray)}
                </div>
            </div>
        `;

        container.innerHTML = html;
        this.openModal('reservationDetailsModal');
    }

    showCancelConfirmation(reservationId) {
        this.selectedReservationId = reservationId;
        const message = document.getElementById('cancelReservationMessage');
        message.textContent = `Tem certeza que deseja cancelar a reserva #${reservationId}?`;
        this.openModal('cancelReservationModal');
    }

    getStatusText(status) {
        const statusMap = {
            'CONFIRMED': 'Confirmada',
            'CANCELLED': 'Cancelada',
            'CHECKED_IN': 'Check-in Realizado',
            'CHECKED_OUT': 'Check-out Realizado'
        };
        return statusMap[status] || status || 'Desconhecido';
    }

    getGuestName(reservation) {
        if (reservation.guest) {
            let guestsArray = [];
            if (Array.isArray(reservation.guest)) {
                guestsArray = reservation.guest;
            } else if (reservation.guest instanceof Set) {
                guestsArray = Array.from(reservation.guest);
            }
            
            if (guestsArray.length > 0 && guestsArray[0].name) {
                return guestsArray[0].name;
            }
        }
        return 'N/A';
    }

    getRoomNumber(reservation) {
        if (reservation.rooms) {
            let roomsArray = [];
            if (Array.isArray(reservation.rooms)) {
                roomsArray = reservation.rooms;
            } else if (reservation.rooms instanceof Set) {
                roomsArray = Array.from(reservation.rooms);
            }
            
            if (roomsArray.length > 0 && roomsArray[0].number) {
                return roomsArray[0].number;
            }
        }
        return 'N/A';
    }

    formatCurrentDates(dates) {
        if (!dates || dates.length === 0) {
            return '<p class="no-data"><i class="fas fa-calendar-times"></i> Nenhuma data definida</p>';
        }
        
        try {
            const sortedDates = dates.sort();

            return sortedDates.map(date => {
                const dateStr = date.substring(0, 10); // Pega apenas YYYY-MM-DD
                const [year, month, day] = dateStr.split('-');
                const parsedDate = new Date(year, month - 1, day);
                const formattedDate = parsedDate.toLocaleDateString('pt-BR', {
                    weekday: 'long',
                    day: '2-digit',
                    month: 'long',
                    year: 'numeric'
                });
                return `
                    <div class="date-item">
                        <i class="fas fa-calendar-day"></i>
                        <span>${formattedDate}</span>
                    </div>
                `;
            }).join('');
        } catch (error) {
            console.error('Erro ao formatar datas:', error);
            return '<p class="error"><i class="fas fa-exclamation-circle"></i> Erro ao carregar datas</p>';
        }
    }

    formatGuestList(guests) {
        if (!guests || guests.length === 0) return '<p class="no-data"><i class="fas fa-user-slash"></i> Nenhum hóspede</p>';
        
        return guests.map(guest => `
            <div class="guest-item">
                <i class="fas fa-user"></i>
                <div class="guest-info">
                    <strong>${guest.name || 'Sem nome'}</strong>
                    ${guest.rg ? `<br><small>RG: ${guest.rg}</small>` : ''}
                    ${guest.phone ? `<br><small>Telefone: ${guest.phone}</small>` : ''}
                    ${guest.email ? `<br><small>Email: ${guest.email}</small>` : ''}
                </div>
            </div>
        `).join('');
    }

    formatGuestDetails(guests) {
        if (!guests || guests.length === 0) return '<p class="no-data"><i class="fas fa-user-slash"></i> Nenhum hóspede</p>';
        
        return `
            <div class="guests-count">
                <i class="fas fa-users"></i> Total: ${guests.length} hóspede(s)
            </div>
            ${guests.map(guest => `
                <div class="guest-detail-card">
                    <div class="guest-header">
                        <i class="fas fa-user-circle"></i>
                        <h5>${guest.name || 'Hóspede sem nome'}</h5>
                    </div>
                    <table class="guest-table">
                        ${guest.rg ? `<tr><th>RG:</th><td>${guest.rg}</td></tr>` : ''}
                        ${guest.phone ? `<tr><th>Telefone:</th><td>${guest.phone}</td></tr>` : ''}
                        ${guest.email ? `<tr><th>Email:</th><td>${guest.email}</td></tr>` : ''}
                        <tr><th>ID:</th><td>${guest.id}</td></tr>
                    </table>
                </div>
            `).join('')}
        `;
    }

    openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'block';
        }
    }

    closeModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'none';
        }
    }

    async performCheckIn(reservationId) {
        try {
            const response = await this.apiRequest(`${this.API_BASE_URL}/reserve/check-in/${reservationId}`, {
                method: 'PUT'
            });

            if (response) {
                this.showAlert('Check-in realizado com sucesso!', 'success');
                this.closeModal('manageModal');
                await this.loadReservations();
            } else {
                throw new Error('Erro ao realizar check-in');
            }
        } catch (error) {
            console.error('Erro no check-in:', error);
            this.showAlert(`Erro no check-in: ${error.message}`, 'error');
        }
    }

    async performCheckOut(reservationId) {
        try {
            const response = await this.apiRequest(`${this.API_BASE_URL}/reserve/check-out/${reservationId}`, {
                method: 'PUT'
            });

            if (response) {
                this.showAlert('Check-out realizado com sucesso!', 'success');
                this.closeModal('manageModal');
                await this.loadReservations();
            } else {
                throw new Error('Erro ao realizar check-out');
            }
        } catch (error) {
            console.error('Erro no check-out:', error);
            this.showAlert(`Erro no check-out: ${error.message}`, 'error');
        }
    }

    async cancelReservationById() {
        if (!this.selectedReservationId) return;

        const cancelBtn = document.getElementById('confirmCancelBtn');
        const originalText = cancelBtn.innerHTML;
        
        cancelBtn.disabled = true;
        cancelBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Cancelando...';

        try {
            const response = await this.apiRequest(`${this.API_BASE_URL}/reserve/cancele/${this.selectedReservationId}`, {
                method: 'PUT'
            });

            if (response) {
                this.showAlert('Reserva cancelada com sucesso!', 'success');
                this.closeModal('cancelReservationModal');
                await this.loadReservations();
                this.selectedReservationId = null;
            } else {
                throw new Error('Erro ao cancelar reserva');
            }
        } catch (error) {
            console.error('Erro ao cancelar reserva:', error);
            this.showAlert(`Erro ao cancelar reserva: ${error.message}`, 'error');
        } finally {
            cancelBtn.disabled = false;
            cancelBtn.innerHTML = originalText;
        }
    }

    previousMonth() {
        this.currentDate.setMonth(this.currentDate.getMonth() - 1);
        this.renderCalendar();
    }

    nextMonth() {
        this.currentDate.setMonth(this.currentDate.getMonth() + 1);
        this.renderCalendar();
    }

    setupEventListeners() {
        document.getElementById('prevMonthBtn').addEventListener('click', () => this.previousMonth());
        document.getElementById('nextMonthBtn').addEventListener('click', () => this.nextMonth());

        document.getElementById('newReservationBtn').addEventListener('click', () => this.openReservationForm());

        document.getElementById('cancelReservationBtn').addEventListener('click', () => this.closeReservationForm());

        document.getElementById('reserveForm').addEventListener('submit', (e) => this.createReservation(e));

        document.getElementById('backToRoomsBtn').addEventListener('click', () => {
            window.location.href = '/room';
        });

        document.getElementById('logoutBtn').addEventListener('click', (e) => {
            e.preventDefault();
            if (confirm('Deseja realmente sair do sistema?')) {
                this.logout();
            }
        });

        document.getElementById('closeManageModal').addEventListener('click', () => {
            this.closeModal('manageModal');
        });

        document.getElementById('closeDetailsModal').addEventListener('click', () => {
            this.closeModal('reservationDetailsModal');
        });

        document.getElementById('closeCancelModal').addEventListener('click', () => {
            this.closeModal('cancelReservationModal');
            this.selectedReservationId = null;
        });

        document.getElementById('cancelCancelBtn').addEventListener('click', () => {
            this.closeModal('cancelReservationModal');
            this.selectedReservationId = null;
        });

        document.getElementById('confirmCancelBtn').addEventListener('click', () => {
            this.cancelReservationById();
        });

        window.addEventListener('click', (event) => {
            const modals = document.querySelectorAll('.modal');
            modals.forEach(modal => {
                if (event.target === modal) {
                    modal.style.display = 'none';
                    if (modal.id === 'cancelReservationModal') {
                        this.selectedReservationId = null;
                    }
                }
            });
        });

        window.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') {
                const modals = document.querySelectorAll('.modal');
                modals.forEach(modal => {
                    if (modal.style.display === 'block') {
                        modal.style.display = 'none';
                        if (modal.id === 'cancelReservationModal') {
                            this.selectedReservationId = null;
                        }
                    }
                });
            }
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.roomCalendar = new RoomCalendar();
});

function showCalendarAlert(message, type = 'info') {
    if (window.roomCalendar) {
        window.roomCalendar.showAlert(message, type);
    }
}