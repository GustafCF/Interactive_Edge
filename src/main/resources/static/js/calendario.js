class CalendarSystem {
    constructor() {
        this.baseUrl = '/reserve';
        this.token = localStorage.getItem('jwtToken');
        this.currentDate = new Date();
        this.selectedDates = new Set();
        this.reservedDates = new Map();
        this.allReservations = [];
        this.allRooms = [];
        this.currentReservationId = null;
        this.expandedDate = null;
        this.initEventListeners();
        this.checkAuthentication();
        this.loadRooms();
        this.loadCalendar();
        this.loadReservations();
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
                userInfoElement.textContent = `Usuário: ${userEmail}`;
            }
        } catch (error) {
            console.error('Erro ao carregar informações do usuário:', error);
        }
    }

    async loadRooms() {
        try {
            const response = await this.makeAuthenticatedRequest('/room/all');
            if (response.ok) {
                this.allRooms = await response.json();
                console.log('Quartos carregados:', this.allRooms.length);
                this.loadCalendar();
            } else {
                console.error('Erro ao carregar quartos');
            }
        } catch (error) {
            console.error('Erro ao carregar quartos:', error);
        }
    }

    initEventListeners() {
        document.getElementById('prevMonth').addEventListener('click', () => this.previousMonth());
        document.getElementById('nextMonth').addEventListener('click', () => this.nextMonth());
        document.getElementById('todayBtn').addEventListener('click', () => this.goToToday());
        document.getElementById('logoutBtn').addEventListener('click', (e) => {
            e.preventDefault();
            this.logout();
        });

        document.getElementById('newReservationBtn').addEventListener('click', () => this.toggleReservationForm());
        document.getElementById('cancelReservationBtn').addEventListener('click', () => this.cancelReservation());
        document.getElementById('reserveForm').addEventListener('submit', (e) => this.handleReservation(e));
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

    // Calendar Functions
    loadCalendar() {
        const calendarTitle = document.getElementById('calendarTitle');
        const calendar = document.getElementById('calendar');
        
        const monthNames = [
            'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
            'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
        ];
        
        calendarTitle.textContent = `${monthNames[this.currentDate.getMonth()]} ${this.currentDate.getFullYear()}`;
        
        calendar.innerHTML = '';
        
        const dayNames = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
        dayNames.forEach(day => {
            const dayHeader = document.createElement('div');
            dayHeader.className = 'calendar-header';
            dayHeader.textContent = day;
            calendar.appendChild(dayHeader);
        });
        
        const firstDay = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth(), 1);
        const lastDay = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() + 1, 0);
        const daysInMonth = lastDay.getDate();
        const startingDay = firstDay.getDay();
        
        const prevMonthLastDay = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth(), 0).getDate();
        for (let i = startingDay - 1; i >= 0; i--) {
            const dayElement = this.createDayElement(
                prevMonthLastDay - i, 
                true, 
                new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() - 1, prevMonthLastDay - i)
            );
            calendar.appendChild(dayElement);
        }
        
        for (let i = 1; i <= daysInMonth; i++) {
            const dayElement = this.createDayElement(
                i, 
                false, 
                new Date(this.currentDate.getFullYear(), this.currentDate.getMonth(), i)
            );
            calendar.appendChild(dayElement);
        }
        
        const totalCells = 42;
        const daysSoFar = startingDay + daysInMonth;
        const daysToAdd = totalCells - daysSoFar;
        
        for (let i = 1; i <= daysToAdd; i++) {
            const dayElement = this.createDayElement(
                i, 
                true, 
                new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() + 1, i)
            );
            calendar.appendChild(dayElement);
        }
    }

    createDayElement(dayNumber, isOtherMonth, date) {
        const dayElement = document.createElement('div');
        dayElement.className = 'calendar-day';
        
        if (isOtherMonth) {
            dayElement.classList.add('other-month');
        }
        
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const compareDate = new Date(date);
        compareDate.setHours(0, 0, 0, 0);
        
        if (compareDate < today) {
            dayElement.classList.add('past');
        }
        
        const dateString = this.formatDate(date);
        const reservedRooms = this.reservedDates.get(dateString) || [];
        const totalRooms = this.allRooms.length;
        
        if (reservedRooms.length > 0) {
            if (reservedRooms.length >= totalRooms) {
                dayElement.classList.add('fully-reserved');
            } else {
                dayElement.classList.add('reserved');
            }
        }
        
        if (this.selectedDates.has(dateString)) {
            dayElement.classList.add('selected');
        }
        
        // Marcar como expandido se for a data atual expandida
        if (this.expandedDate === dateString) {
            dayElement.classList.add('expanded');
        }
        
        const dayNumberElement = document.createElement('div');
        dayNumberElement.className = 'calendar-day-number';
        dayNumberElement.textContent = dayNumber;
        dayElement.appendChild(dayNumberElement);
        
        const eventsElement = document.createElement('div');
        eventsElement.className = 'calendar-day-events';
        
        // Mostrar apenas informações resumidas quando não expandido
        if (this.expandedDate !== dateString) {
            const reservationsForDate = this.getReservationsForDate(dateString);
            
            if (reservationsForDate.length > 0) {
                const reservedCount = reservationsForDate.length;
                const eventElement = document.createElement('div');
                eventElement.className = 'event';
                eventElement.textContent = `${reservedCount} reserva(s)`;
                eventElement.title = `Clique para ver detalhes das reservas`;
                eventsElement.appendChild(eventElement);
            }
            
            // Mostrar disponibilidade apenas se houver reservas
            if (reservedRooms.length > 0 && reservedRooms.length < totalRooms) {
                const availableRooms = totalRooms - reservedRooms.length;
                const availabilityElement = document.createElement('div');
                availabilityElement.className = 'event available';
                availabilityElement.textContent = `${availableRooms} livre(s)`;
                availabilityElement.title = `${availableRooms} quarto(s) disponível(is)`;
                eventsElement.appendChild(availabilityElement);
            }
            
            // Adicionar indicador visual para datas selecionáveis quando o formulário estiver visível
            const isFormVisible = document.getElementById('reservationForm').style.display === 'block';
            if (isFormVisible && !dayElement.classList.contains('past') && !dayElement.classList.contains('fully-reserved')) {
                const selectIndicator = document.createElement('div');
                selectIndicator.className = 'event available';
                // selectIndicator.textContent = '📅 Clique para selecionar';
                selectIndicator.style.fontSize = '0.6rem';
                selectIndicator.style.marginTop = '2px';
                selectIndicator.style.background = 'var(--blue-light)';
                selectIndicator.style.color = 'white';
                eventsElement.appendChild(selectIndicator);
            }
        } else {
            // Mostrar detalhes expandidos quando a data estiver clicada
            this.createExpandedDateContent(dateString, eventsElement);
        }
        
        dayElement.appendChild(eventsElement);
        
        const isSelectable = !dayElement.classList.contains('past') && !dayElement.classList.contains('fully-reserved');
        
        if (isSelectable) {
            dayElement.style.cursor = 'pointer';
            dayElement.addEventListener('click', (e) => {
                e.stopPropagation();
                this.toggleDateExpansion(date);
            });
        } else {
            dayElement.style.cursor = 'not-allowed';
        }
        
        return dayElement;
    }

    createExpandedDateContent(dateString, container) {
        const reservedRooms = this.reservedDates.get(dateString) || [];
        const totalRooms = this.allRooms.length;
        const availableRooms = totalRooms - reservedRooms.length;
        
        // Obter todas as reservas para esta data
        const reservationsForDate = this.getReservationsForDate(dateString);
        
        // Mostrar quartos ocupados
        if (reservationsForDate.length > 0) {
            const occupiedTitle = document.createElement('div');
            occupiedTitle.className = 'event-title';
            occupiedTitle.textContent = 'Quartos Ocupados:';
            occupiedTitle.style.fontWeight = 'bold';
            occupiedTitle.style.marginBottom = '2px';
            occupiedTitle.style.fontSize = '0.7rem';
            container.appendChild(occupiedTitle);
            
            reservationsForDate.forEach(reservation => {
                const roomNumber = this.getRoomNumber(reservation);
                const guestName = this.getGuestName(reservation);
                const status = this.getStatusText(reservation.reserveStatus);
                
                const eventElement = document.createElement('div');
                eventElement.className = 'event occupied';
                eventElement.innerHTML = `
                    <strong>Q${roomNumber}</strong>: ${guestName}
                    <br><small>Status: ${status}</small>
                `;
                eventElement.title = `Hóspede: ${guestName} - Status: ${status}`;
                container.appendChild(eventElement);
            });
        }
        
        // Mostrar quartos disponíveis apenas se houver reservas
        if (reservedRooms.length > 0 && availableRooms > 0) {
            const availableTitle = document.createElement('div');
            availableTitle.className = 'event-title';
            availableTitle.textContent = 'Quartos Disponíveis:';
            availableTitle.style.fontWeight = 'bold';
            availableTitle.style.margin = '4px 0 2px 0';
            availableTitle.style.fontSize = '0.7rem';
            container.appendChild(availableTitle);
            
            // Encontrar quartos disponíveis
            const allRoomNumbers = this.allRooms.map(room => room.number);
            const occupiedRoomNumbers = reservedRooms;
            const availableRoomNumbers = allRoomNumbers.filter(roomNum => 
                !occupiedRoomNumbers.includes(roomNum)
            );
            
            // Agrupar quartos disponíveis para não sobrecarregar a visualização
            if (availableRoomNumbers.length <= 5) {
                // Mostrar todos os números se forem poucos
                availableRoomNumbers.forEach(roomNum => {
                    const availableElement = document.createElement('div');
                    availableElement.className = 'event available';
                    availableElement.textContent = `Quarto ${roomNum}`;
                    availableElement.title = `Quarto ${roomNum} disponível`;
                    container.appendChild(availableElement);
                });
            } else {
                // Mostrar resumo se forem muitos quartos
                const availableElement = document.createElement('div');
                availableElement.className = 'event available';
                availableElement.textContent = `${availableRooms} quartos disponíveis`;
                availableElement.title = `Quartos disponíveis: ${availableRoomNumbers.join(', ')}`;
                container.appendChild(availableElement);
            }
        }
    }

    toggleDateExpansion(date) {
        const dateString = this.formatDate(date);
        
        // Verificar se estamos no modo de seleção (formulário visível)
        const isReservationFormVisible = document.getElementById('reservationForm').style.display === 'block';
        
        if (isReservationFormVisible) {
            // Modo seleção: alternar seleção da data
            this.toggleDateSelection(date);
        } else {
            // Modo visualização: alternar expansão
            if (this.expandedDate === dateString) {
                this.expandedDate = null;
                this.hideReservationsList();
            } else {
                this.expandedDate = dateString;
                this.showReservationsForDate(dateString);
            }
            this.loadCalendar();
        }
    }

    toggleDateSelection(date) {
        const dateString = this.formatDate(date);
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        // Não permitir selecionar datas passadas
        if (date < today) {
            this.showAlert('Não é possível selecionar datas passadas', 'error');
            return;
        }
        
        // Verificar se a data está totalmente reservada
        const reservedRooms = this.reservedDates.get(dateString) || [];
        const totalRooms = this.allRooms.length;
        
        if (reservedRooms.length >= totalRooms) {
            this.showAlert('Esta data está completamente ocupada', 'error');
            return;
        }
        
        // Alternar seleção
        if (this.selectedDates.has(dateString)) {
            this.selectedDates.delete(dateString);
        } else {
            this.selectedDates.add(dateString);
        }
        
        this.updateSelectedDatesDisplay();
        this.loadCalendar();
    }

    showReservationsForDate(dateString) {
        const reservationsForDate = this.getReservationsForDate(dateString);
        const container = document.getElementById('reservationsContainer');
        const reservationsCard = document.querySelector('.reservations-list');
        
        if (reservationsForDate.length === 0) {
            container.innerHTML = '<p>Nenhuma reserva encontrada para esta data</p>';
        } else {
            const html = reservationsForDate.map(reservation => `
                <div class="reservation-item">
                    <div class="reservation-header">
                        <div>
                            <strong>Reserva #${reservation.id}</strong>
                            <span class="badge badge-${this.getStatusClass(reservation.reserveStatus)}">
                                ${this.getStatusText(reservation.reserveStatus)}
                            </span>
                        </div>
                        <div class="reservation-actions">
                            <button class="btn btn-sm btn-info" onclick="calendarSystem.manageReservation(${reservation.id})">
                                👁️ Gerenciar
                            </button>
                            ${reservation.reserveStatus === 'CONFIRMED' ? `
                                <button class="btn btn-sm btn-success" onclick="calendarSystem.performCheckIn(${reservation.id})">
                                    ✅ Check-in
                                </button>
                                <button class="btn btn-sm btn-danger" onclick="calendarSystem.cancelReservationById(${reservation.id})">
                                    ❌ Cancelar
                                </button>
                            ` : ''}
                            ${reservation.reserveStatus === 'CHECKED_IN' ? `
                                <button class="btn btn-sm btn-primary" onclick="calendarSystem.performCheckOut(${reservation.id})">
                                    🏁 Check-out
                                </button>
                            ` : ''}
                        </div>
                    </div>
                    <div>
                        <strong>Hóspede:</strong> ${this.getGuestName(reservation)}<br>
                        <strong>Quarto:</strong> ${this.getRoomNumber(reservation)}<br>
                        <strong>Datas:</strong> ${reservation.reservedDays ? Array.from(reservation.reservedDays).sort().join(', ') : 'N/A'}<br>
                        ${reservation.checkIn && reservation.checkIn.length > 0 ? `<strong>Check-in:</strong> ${new Date(reservation.checkIn[0]).toLocaleString('pt-BR')}<br>` : ''}
                        ${reservation.checkOut && reservation.checkOut.length > 0 ? `<strong>Check-out:</strong> ${new Date(reservation.checkOut[0]).toLocaleString('pt-BR')}` : ''}
                    </div>
                </div>
            `).join('');

            container.innerHTML = html;
        }
        
        // Mostrar o card de reservas
        reservationsCard.style.display = 'block';
        
        // Rolar até as reservas
        reservationsCard.scrollIntoView({ behavior: 'smooth' });
    }

    hideReservationsList() {
        const reservationsCard = document.querySelector('.reservations-list');
        reservationsCard.style.display = 'none';
    }

    getReservationsForDate(dateString) {
        return this.allReservations.filter(reservation => 
            reservation.reservedDays && 
            Array.from(reservation.reservedDays).includes(dateString) &&
            reservation.reserveStatus !== 'CANCELLED'
        );
    }

    getRoomNumber(reservation) {
        if (reservation.rooms && reservation.rooms.length > 0) {
            return reservation.rooms[0].number;
        }
        return 'N/A';
    }

    getGuestName(reservation) {
        if (reservation.guest && reservation.guest.length > 0) {
            return reservation.guest[0].name;
        }
        return 'N/A';
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

    updateSelectedDatesDisplay() {
        const selectedDatesContainer = document.getElementById('selectedDates');
        selectedDatesContainer.innerHTML = '';
        
        const sortedDates = Array.from(this.selectedDates).sort();
        
        sortedDates.forEach(dateString => {
            const dateTag = document.createElement('div');
            dateTag.className = 'date-tag';
            
            const date = this.parseDate(dateString);
            const formattedDate = date.toLocaleDateString('pt-BR');
            
            dateTag.innerHTML = `
                ${formattedDate}
                <button type="button" data-date="${dateString}">×</button>
            `;
            
            const removeButton = dateTag.querySelector('button');
            removeButton.addEventListener('click', (e) => {
                e.stopPropagation();
                this.selectedDates.delete(dateString);
                this.updateSelectedDatesDisplay();
                this.loadCalendar();
            });
            
            selectedDatesContainer.appendChild(dateTag);
        });
    }

    previousMonth() {
        this.currentDate.setMonth(this.currentDate.getMonth() - 1);
        this.loadCalendar();
    }

    nextMonth() {
        this.currentDate.setMonth(this.currentDate.getMonth() + 1);
        this.loadCalendar();
    }

    goToToday() {
        this.currentDate = new Date();
        this.loadCalendar();
    }

    toggleReservationForm() {
        const form = document.getElementById('reservationForm');
        const isFormVisible = form.style.display === 'block';
        
        if (isFormVisible) {
            form.style.display = 'none';
            this.expandedDate = null; // Sair do modo expansão
        } else {
            form.style.display = 'block';
            this.selectedDates.clear();
            this.expandedDate = null; // Garantir que não está em modo expansão
            this.hideReservationsList();
        }
        
        this.updateSelectedDatesDisplay();
        this.loadCalendar(); // Recarregar para mostrar os indicadores de seleção
    }

    cancelReservation() {
        document.getElementById('reservationForm').style.display = 'none';
        document.getElementById('reserveForm').reset();
        this.selectedDates.clear();
        this.updateSelectedDatesDisplay();
        this.loadCalendar();
    }

    async loadReservations() {
        try {
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/all`);
            
            if (response.ok) {
                const reserves = await response.json();
                this.allReservations = reserves;
                this.updateReservedDates();
                // Não mostrar reservas inicialmente
                this.hideReservationsList();
                this.loadCalendar();
            } else {
                throw new Error('Erro ao carregar reservas');
            }
        } catch (error) {
            console.error('Erro ao carregar reservas:', error);
            this.showAlert('Erro ao carregar reservas existentes', 'error');
        }
    }

    updateReservedDates() {
        this.reservedDates.clear();
        
        this.allReservations.forEach(reserve => {
            if (reserve.reservedDays && reserve.reserveStatus !== 'CANCELLED') {
                const roomNumber = this.getRoomNumber(reserve);
                reserve.reservedDays.forEach(date => {
                    if (!this.reservedDates.has(date)) {
                        this.reservedDates.set(date, []);
                    }
                    if (roomNumber !== 'N/A') {
                        this.reservedDates.get(date).push(roomNumber);
                    }
                });
            }
        });
    }

    getStatusClass(status) {
        const classMap = {
            'CONFIRMED': 'confirmed',
            'CANCELLED': 'cancelled',
            'CHECKED_IN': 'checked-in',
            'CHECKED_OUT': 'checked-out'
        };
        return classMap[status] || 'confirmed';
    }

    getStatusText(status) {
        const statusMap = {
            'CONFIRMED': 'Confirmada',
            'CANCELLED': 'Cancelada',
            'CHECKED_IN': 'Check-in Realizado',
            'CHECKED_OUT': 'Check-out Realizado'
        };
        return statusMap[status] || status;
    }

    async handleReservation(event) {
        event.preventDefault();
        
        const guestName = document.getElementById('guestName').value;
        const roomNumber = parseInt(document.getElementById('roomNumber').value);
        
        if (this.selectedDates.size === 0) {
            this.showAlert('Selecione pelo menos uma data para reservar', 'error');
            return;
        }
        
        if (!guestName || !roomNumber) {
            this.showAlert('Preencha todos os campos obrigatórios', 'error');
            return;
        }
        
        const reserveBtn = document.getElementById('reserveBtn');
        reserveBtn.disabled = true;
        reserveBtn.innerHTML = '<div class="loading"></div> Reservando...';
        
        try {
            const datesArray = Array.from(this.selectedDates).map(dateStr => {
                const date = this.parseDate(dateStr);
                return this.formatDate(date);
            });
            
            const reservationData = {
                dates: datesArray,
                guestName: guestName,
                roomNumber: roomNumber
            };
            
            console.log('Enviando dados:', reservationData);
            
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/insert`, {
                method: 'POST',
                body: JSON.stringify(reservationData)
            });
            
            if (response.ok) {
                const reserve = await response.json();
                this.showAlert('Reserva criada com sucesso!', 'success');
                this.cancelReservation();
                this.loadReservations();
            } else {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Erro ao criar reserva');
            }
        } catch (error) {
            console.error('Erro ao criar reserva:', error);
            this.showAlert(`Erro ao criar reserva: ${error.message}`, 'error');
        } finally {
            reserveBtn.disabled = false;
            reserveBtn.innerHTML = 'Reservar';
        }
    }

    async performCheckIn(reservationId) {
        try {
            const result = await Swal.fire({
                title: 'Confirmar Check-in',
                text: 'Deseja realizar o check-in desta reserva?',
                icon: 'question',
                showCancelButton: true,
                confirmButtonText: 'Sim, fazer check-in',
                cancelButtonText: 'Cancelar'
            });
            
            if (result.isConfirmed) {
                const response = await this.makeAuthenticatedRequest(
                    `${this.baseUrl}/check-in/${reservationId}`,
                    { method: 'PUT' }
                );

                if (response.ok) {
                    await response.json();
                    this.showAlert('Check-in realizado com sucesso!', 'success');
                    this.loadReservations();
                } else {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Erro ao realizar check-in');
                }
            }
        } catch (error) {
            console.error('Erro no check-in:', error);
            this.showAlert(`Erro no check-in: ${error.message}`, 'error');
        }
    }

    async performCheckOut(reservationId) {
        try {
            const result = await Swal.fire({
                title: 'Confirmar Check-out',
                text: 'Deseja realizar o check-out desta reserva?',
                icon: 'question',
                showCancelButton: true,
                confirmButtonText: 'Sim, fazer check-out',
                cancelButtonText: 'Cancelar'
            });
            
            if (result.isConfirmed) {
                const response = await this.makeAuthenticatedRequest(
                    `${this.baseUrl}/check-out/${reservationId}`,
                    { method: 'PUT' }
                );

                if (response.ok) {
                    await response.json();
                    this.showAlert('Check-out realizado com sucesso!', 'success');
                    this.loadReservations();
                } else {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Erro ao realizar check-out');
                }
            }
        } catch (error) {
            console.error('Erro no check-out:', error);
            this.showAlert(`Erro no check-out: ${error.message}`, 'error');
        }
    }

    async cancelReservationById(reservationId) {
        try {
            const result = await Swal.fire({
                title: 'Confirmar Cancelamento',
                text: 'Tem certeza que deseja cancelar esta reserva? Esta ação não pode ser desfeita.',
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: 'Sim, cancelar',
                cancelButtonText: 'Manter reserva',
                confirmButtonColor: '#d33'
            });
            
            if (result.isConfirmed) {
                const response = await this.makeAuthenticatedRequest(
                    `${this.baseUrl}/cancele/${reservationId}`,
                    { method: 'PUT' }
                );

                if (response.ok) {
                    await response.json();
                    this.showAlert('Reserva cancelada com sucesso!', 'success');
                    this.loadReservations();
                } else {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Erro ao cancelar reserva');
                }
            }
        } catch (error) {
            console.error('Erro ao cancelar reserva:', error);
            this.showAlert(`Erro ao cancelar reserva: ${error.message}`, 'error');
        }
    }

    async manageReservation(reservationId) {
        try {
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/find/${reservationId}`);
            
            if (response.ok) {
                const reservation = await response.json();
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
        
        const html = `
            <div>
                <!-- Cabeçalho com Informações Principais -->
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
                </div>

                <!-- Status da Reserva -->
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
                        <div class="quick-actions" style="display: flex; gap: 0.5rem;">
                            ${reservation.reserveStatus !== 'CANCELLED' && reservation.checkIn && reservation.checkIn.length === 0 ? `
                                <button class="btn btn-success btn-sm" onclick="calendarSystem.performCheckIn(${reservation.id})">
                                    ✅ Check-in
                                </button>
                            ` : ''}
                            ${reservation.checkIn && reservation.checkIn.length > 0 && reservation.checkOut && reservation.checkOut.length === 0 ? `
                                <button class="btn btn-info btn-sm" onclick="calendarSystem.performCheckOut(${reservation.id})">
                                    🏁 Check-out
                                </button>
                            ` : ''}
                            ${reservation.reserveStatus !== 'CANCELLED' ? `
                                <button class="btn btn-danger btn-sm" onclick="calendarSystem.cancelReservationById(${reservation.id})">
                                    ❌ Cancelar
                                </button>
                            ` : ''}
                        </div>
                    </div>
                </div>

                <!-- Ações Rápidas -->
                <div class="management-section">
                    <h4 class="section-title">⚡ Ações Rápidas</h4>
                    <div class="action-grid">
                        <div class="action-card" onclick="calendarSystem.openAddDatesModal(${reservation.id})">
                            <div class="action-icon">📅</div>
                            <div class="action-title">Adicionar Datas</div>
                            <div class="action-description">Incluir novas datas na reserva</div>
                        </div>
                        <div class="action-card" onclick="calendarSystem.openAddRoomModal(${reservation.id})">
                            <div class="action-icon">🏠</div>
                            <div class="action-title">Adicionar Quarto</div>
                            <div class="action-description">Incluir outro quarto na reserva</div>
                        </div>
                        <div class="action-card" onclick="calendarSystem.focusAddGuest(${reservation.id})">
                            <div class="action-icon">👥</div>
                            <div class="action-title">Adicionar Hóspede</div>
                            <div class="action-description">Incluir novo hóspede</div>
                        </div>
                    </div>
                </div>

                <!-- Gerenciamento de Datas -->
                <div class="management-section">
                    <h4 class="section-title">📅 Datas da Reserva</h4>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                        <div>
                            <h5>Datas Atuais</h5>
                            <div class="current-dates">
                                ${this.formatCurrentDates(reservation.reservedDays, reservation.id)}
                            </div>
                        </div>
                        <div>
                            <h5>Adicionar Data Rápida</h5>
                            <div class="date-picker-group">
                                <div class="form-group" style="flex: 1;">
                                    <input type="date" id="quickAddDate" class="form-control">
                                </div>
                                <button class="btn btn-success btn-sm" onclick="calendarSystem.quickAddDate(${reservation.id})">
                                    +
                                </button>
                            </div>
                            <small style="color: #666; display: block; margin-top: 0.5rem;">
                                Clique no botão "+" para adicionar rapidamente uma data
                            </small>
                        </div>
                    </div>
                </div>

                <!-- Gerenciamento de Hóspedes -->
                <div class="management-section">
                    <h4 class="section-title">👥 Hóspedes</h4>
                    <div class="guest-list" id="currentGuestList">
                        ${this.formatGuestList(reservation.guest, reservation.id)}
                    </div>
                    <div class="form-group" style="margin-top: 1rem;">
                        <div class="date-picker-group">
                            <input type="text" id="newGuestName" class="form-control" placeholder="Nome do novo hóspede" style="flex: 1;">
                            <button class="btn btn-success btn-sm" onclick="calendarSystem.addGuest(${reservation.id})">
                                Adicionar
                            </button>
                        </div>
                    </div>
                </div>

                <!-- Gerenciamento de Quartos -->
                <div class="management-section">
                    <h4 class="section-title">🏠 Quartos</h4>
                    <div class="guest-list">
                        ${reservation.rooms && reservation.rooms.length > 0 ? 
                            reservation.rooms.map(room => `
                                <div class="guest-item">
                                    <span>Quarto ${room.number} - ${room.exclusiveRoom ? '🔄 Exclusivo' : '👥 Compartilhado'}</span>
                                    ${reservation.rooms.length > 1 ? `
                                        <button class="btn btn-sm btn-danger" onclick="calendarSystem.removeRoom(${reservation.id}, ${room.number})">
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
        document.getElementById('manageModal').style.display = 'block';
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
                        <button class="btn btn-sm btn-danger" onclick="calendarSystem.removeDate(${reservationId}, '${date}')">
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
                <button class="btn btn-sm btn-danger" onclick="calendarSystem.removeGuest(${reservationId}, '${this.escapeHtml(guest.name)}')">
                    Remover
                </button>
            </div>
        `).join('');
    }

    closeManageModal() {
        document.getElementById('manageModal').style.display = 'none';
        this.currentReservationId = null;
    }

    // Métodos para gerenciamento de datas
    openAddDatesModal(reservationId) {
        this.currentReservationId = reservationId;
        document.getElementById('addDatesModal').style.display = 'block';
        
        // Set default dates
        const today = new Date();
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        
        document.getElementById('newSingleDate').value = this.formatDate(today);
        document.getElementById('newCheckInDate').value = this.formatDate(today);
        document.getElementById('newCheckOutDate').value = this.formatDate(tomorrow);
    }

    closeAddDatesModal() {
        document.getElementById('addDatesModal').style.display = 'none';
        document.getElementById('dateAvailabilityResult').innerHTML = '';
    }

    async checkDateAvailability() {
        const singleDate = document.getElementById('newSingleDate').value;
        const checkIn = document.getElementById('newCheckInDate').value;
        const checkOut = document.getElementById('newCheckOutDate').value;

        if (!singleDate && (!checkIn || !checkOut)) {
            this.showAlert('Por favor, preencha pelo menos uma data ou período', 'error');
            return;
        }

        try {
            let dateToCheck = singleDate;
            if (!singleDate && checkIn) {
                dateToCheck = checkIn;
            }

            // Simular verificação de disponibilidade
            // Em um sistema real, você faria uma requisição para o backend
            const isAvailable = true; // Simulação
            
            const container = document.getElementById('dateAvailabilityResult');
            const html = `
                <div class="alert ${isAvailable ? 'alert-success' : 'alert-error'}">
                    <strong>${isAvailable ? 'Data disponível!' : 'Data não disponível'}</strong>
                </div>
            `;

            container.innerHTML = html;
        } catch (error) {
            console.error('Erro ao verificar disponibilidade:', error);
            this.showAlert('Erro ao verificar disponibilidade da data', 'error');
        }
    }

    async addSingleDate() {
        const newDate = document.getElementById('newSingleDate').value;
        
        if (!newDate) {
            this.showAlert('Por favor, selecione uma data', 'error');
            return;
        }

        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/${this.currentReservationId}/add-date`,
                {
                    method: 'PUT',
                    body: JSON.stringify(newDate)
                }
            );

            if (response.ok) {
                this.showAlert('Data adicionada com sucesso!', 'success');
                this.closeAddDatesModal();
                await this.manageReservation(this.currentReservationId);
            } else {
                throw new Error('Erro ao adicionar data');
            }
        } catch (error) {
            console.error('Erro ao adicionar data:', error);
            this.showAlert('Erro ao adicionar data: ' + error.message, 'error');
        }
    }

    async addDateRange() {
        const checkIn = document.getElementById('newCheckInDate').value;
        const checkOut = document.getElementById('newCheckOutDate').value;

        if (!checkIn || !checkOut) {
            this.showAlert('Por favor, preencha ambas as datas do período', 'error');
            return;
        }

        const dates = this.getDatesBetween(new Date(checkIn), new Date(checkOut));
        const dateStrings = dates.map(date => date.toISOString().split('T')[0]);

        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/${this.currentReservationId}/add-dates`,
                {
                    method: 'PUT',
                    body: JSON.stringify({ dates: dateStrings })
                }
            );

            if (response.ok) {
                this.showAlert('Período adicionado com sucesso!', 'success');
                this.closeAddDatesModal();
                await this.manageReservation(this.currentReservationId);
            } else {
                throw new Error('Erro ao adicionar período');
            }
        } catch (error) {
            console.error('Erro ao adicionar período:', error);
            this.showAlert('Erro ao adicionar período: ' + error.message, 'error');
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
            } else {
                throw new Error('Erro ao adicionar data');
            }
        } catch (error) {
            console.error('Erro ao adicionar data:', error);
            this.showAlert('Erro ao adicionar data: ' + error.message, 'error');
        }
    }

    async removeDate(reservationId, dateToRemove) {
        if (!confirm(`Tem certeza que deseja remover a data ${dateToRemove}?`)) {
            return;
        }

        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/remove-date/${reservationId}?date=${dateToRemove}`,
                { method: 'PUT' }
            );

            if (response.ok) {
                this.showAlert('Data removida com sucesso!', 'success');
                await this.manageReservation(reservationId);
            } else {
                throw new Error('Erro ao remover data');
            }
        } catch (error) {
            console.error('Erro ao remover data:', error);
            this.showAlert('Erro ao remover data: ' + error.message, 'error');
        }
    }

    // Métodos para gerenciamento de quartos
    openAddRoomModal(reservationId) {
        this.currentReservationId = reservationId;
        document.getElementById('addRoomModal').style.display = 'block';
    }

    closeAddRoomModal() {
        document.getElementById('addRoomModal').style.display = 'none';
    }

    async addRoom() {
        const roomNumber = parseInt(document.getElementById('newRoomNumber').value);
        
        if (!roomNumber) {
            this.showAlert('Por favor, digite o número do quarto', 'error');
            return;
        }

        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/add-room/${this.currentReservationId}?roomNumber=${roomNumber}`,
                { method: 'PUT' }
            );

            if (response.ok) {
                this.showAlert('Quarto adicionado com sucesso!', 'success');
                this.closeAddRoomModal();
                await this.manageReservation(this.currentReservationId);
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
                `${this.baseUrl}/remove-room/${reservationId}?roomNumber=${roomNumber}`,
                { method: 'PUT' }
            );

            if (response.ok) {
                this.showAlert('Quarto removido com sucesso!', 'success');
                await this.manageReservation(reservationId);
            } else {
                throw new Error('Erro ao remover quarto');
            }
        } catch (error) {
            console.error('Erro ao remover quarto:', error);
            this.showAlert('Erro ao remover quarto: ' + error.message, 'error');
        }
    }

    // Métodos para gerenciamento de hóspedes
    focusAddGuest(reservationId) {
        this.currentReservationId = reservationId;
        const newGuestInput = document.getElementById('newGuestName');
        if (newGuestInput) {
            newGuestInput.focus();
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
                await this.manageReservation(reservationId);
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
                await this.manageReservation(reservationId);
            } else {
                throw new Error('Erro ao remover hóspede');
            }
        } catch (error) {
            console.error('Erro ao remover hóspede:', error);
            this.showAlert('Erro ao remover hóspede', 'error');
        }
    }

    getDatesBetween(startDate, endDate) {
        const dates = [];
        const currentDate = new Date(startDate);
        const finalDate = new Date(endDate);

        while (currentDate < finalDate) {
            dates.push(new Date(currentDate));
            currentDate.setDate(currentDate.getDate() + 1);
        }

        return dates;
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    showAlert(message, type) {
        document.getElementById('alertError').style.display = 'none';
        document.getElementById('alertSuccess').style.display = 'none';
        
        const alertElement = document.getElementById(`alert${type.charAt(0).toUpperCase() + type.slice(1)}`);
        alertElement.textContent = message;
        alertElement.style.display = 'block';
        
        if (type === 'success') {
            setTimeout(() => {
                alertElement.style.display = 'none';
            }, 5000);
        }
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.calendarSystem = new CalendarSystem();
});

// Fechar modal ao clicar fora
window.onclick = function(event) {
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });
    
    // Recolher data expandida ao clicar fora do calendário
    if (window.calendarSystem && window.calendarSystem.expandedDate) {
        const calendar = document.getElementById('calendar');
        if (!calendar.contains(event.target)) {
            window.calendarSystem.expandedDate = null;
            window.calendarSystem.hideReservationsList();
            window.calendarSystem.loadCalendar();
        }
    }
};