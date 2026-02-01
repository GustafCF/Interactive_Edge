class CalendarSystem {
    constructor() {
        this.baseUrl = '/reserve';
        this.token = localStorage.getItem('jwtToken');
        this.currentDate = new Date();
        this.selectedDates = new Set();
        this.reservedDates = new Map();
        this.allReservations = [];
        this.allRooms = [];
        this.allGuests = [];
        this.currentReservationId = null;
        this.expandedDate = null;
        this.guestSearchTimeout = null;
        this.roomSearchTimeout = null;
        this.filteredGuests = [];
        this.filteredRooms = [];
        this.selectedGuest = null;
        this.selectedRoom = null;
        this.newGuestSearchTimeout = null;
        this.pendingReservationData = null;
        
        this.initEventListeners();
        this.checkAuthentication();
        this.loadGuests();
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

    loadUserInfo() {
        try {
            const userInfoElement = document.getElementById('userInfo');
            const userEmail = localStorage.getItem('userInfo');
            const userName = localStorage.getItem('userName');
            
            if (userInfoElement) {
                const displayName = userName || userEmail || 'Usuário';
                userInfoElement.innerHTML = `<i class="fas fa-user"></i> ${displayName}`;
            }
        } catch (error) {
            console.error('Erro ao carregar informações do usuário:', error);
        }
    }

    async loadGuests() {
        try {
            const response = await this.makeAuthenticatedRequest('/guest/all');
            if (response.ok) {
                this.allGuests = await response.json();
            } else {
                console.error('Erro ao carregar hóspedes');
            }
        } catch (error) {
            console.error('Erro ao carregar hóspedes:', error);
        }
    }

    async loadRooms() {
        try {
            const response = await this.makeAuthenticatedRequest('/room/all');
            if (response.ok) {
                this.allRooms = await response.json();
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
        
        this.initAutocompleteListeners();
        setTimeout(() => this.showReservationPreview(), 500);
    }

    initAutocompleteListeners() {
        const guestInput = document.getElementById('guestName');
        const roomInput = document.getElementById('roomNumber');

        guestInput.addEventListener('input', (e) => this.searchGuests(e.target.value));
        guestInput.addEventListener('keydown', (e) => this.handleGuestKeydown(e));
        guestInput.addEventListener('focus', () => this.showGuestSuggestions());
        guestInput.addEventListener('blur', () => {
            setTimeout(() => this.hideGuestSuggestions(), 200);
        });

        roomInput.addEventListener('input', (e) => this.searchRooms(e.target.value));
        roomInput.addEventListener('keydown', (e) => this.handleRoomKeydown(e));
        roomInput.addEventListener('focus', () => this.showRoomSuggestions());
        roomInput.addEventListener('blur', () => {
            setTimeout(() => this.hideRoomSuggestions(), 200);
        });
    }

    async searchGuests(query) {
        clearTimeout(this.guestSearchTimeout);
        
        this.guestSearchTimeout = setTimeout(() => {
            if (!query || query.length < 2) {
                this.filteredGuests = this.allGuests.slice(0, 10);
                this.showGuestSuggestions();
                return;
            }

            const lowerQuery = query.toLowerCase();
            this.filteredGuests = this.allGuests.filter(guest => 
                guest.name.toLowerCase().includes(lowerQuery) ||
                (guest.rg && guest.rg.toLowerCase().includes(lowerQuery)) ||
                (guest.email && guest.email.toLowerCase().includes(lowerQuery)) ||
                (guest.phone && guest.phone.toLowerCase().includes(lowerQuery))
            ).slice(0, 10);

            this.showGuestSuggestions();
            this.updatePreview();
        }, 300);
    }

    async searchRooms(query) {
        clearTimeout(this.roomSearchTimeout);
        
        this.roomSearchTimeout = setTimeout(() => {
            if (!query) {
                this.filteredRooms = this.allRooms
                    .slice(0, 10)
                    .sort((a, b) => a.number - b.number);
                this.showRoomSuggestions();
                this.updatePreview();
                return;
            }

            const roomNumber = parseInt(query);
            if (isNaN(roomNumber)) {
                const lowerQuery = query.toLowerCase();
                this.filteredRooms = this.allRooms.filter(room => 
                    room.number.toString().includes(query) ||
                    (room.roomTypeDescription && 
                     room.roomTypeDescription.toLowerCase().includes(lowerQuery))
                ).slice(0, 10);
                this.showRoomSuggestions();
                this.updatePreview();
                return;
            }

            this.filteredRooms = this.allRooms.filter(room => 
                room.number.toString().includes(query)
            ).slice(0, 10);

            this.showRoomSuggestions();
            this.updatePreview();
        }, 300);
    }

    showGuestSuggestions() {
        const suggestionsContainer = document.getElementById('guestSuggestions');
        const guestInput = document.getElementById('guestName');
        
        if (this.filteredGuests.length === 0) {
            suggestionsContainer.classList.remove('show');
            return;
        }

        const suggestionsHTML = this.filteredGuests.map((guest, index) => `
            <div class="suggestion-item ${index === 0 ? 'highlighted' : ''}" 
                 data-guest-id="${guest.id}"
                 data-guest-name="${this.escapeHtml(guest.name)}"
                 data-guest-rg="${this.escapeHtml(guest.rg || '')}"
                 data-guest-phone="${this.escapeHtml(guest.phone || '')}">
                <div><strong>${this.escapeHtml(guest.name)}</strong></div>
                ${guest.rg ? `<div class="suggestion-details">RG: ${this.escapeHtml(guest.rg)}</div>` : ''}
                ${guest.phone ? `<div class="suggestion-details">Tel: ${this.escapeHtml(guest.phone)}</div>` : ''}
            </div>
        `).join('');

        suggestionsContainer.innerHTML = suggestionsHTML;
        suggestionsContainer.classList.add('show');

        suggestionsContainer.querySelectorAll('.suggestion-item').forEach(item => {
            item.addEventListener('click', (e) => {
                const guestName = e.currentTarget.getAttribute('data-guest-name');
                guestInput.value = guestName;
                guestInput.classList.add('selected');
                this.selectedGuest = guestName;
                this.hideGuestSuggestions();
                this.updatePreview();
            });
        });
    }

    showRoomSuggestions() {
        const suggestionsContainer = document.getElementById('roomSuggestions');
        const roomInput = document.getElementById('roomNumber');
        
        if (this.filteredRooms.length === 0) {
            suggestionsContainer.classList.remove('show');
            return;
        }

        const suggestionsHTML = this.filteredRooms.map((room, index) => `
            <div class="suggestion-item ${index === 0 ? 'highlighted' : ''}" 
                 data-room-number="${room.number}"
                 data-room-type="${room.roomType || 'N/A'}"
                 data-room-price="${room.price || 0}">
                <div><strong>Quarto ${room.number}</strong></div>
                <div class="suggestion-details">
                    ${room.roomTypeDescription || 'Tipo: N/A'} | 
                    R$ ${room.price ? parseFloat(room.price).toFixed(2) : '0,00'}
                </div>
            </div>
        `).join('');

        suggestionsContainer.innerHTML = suggestionsHTML;
        suggestionsContainer.classList.add('show');

        suggestionsContainer.querySelectorAll('.suggestion-item').forEach(item => {
            item.addEventListener('click', (e) => {
                const roomNumber = e.currentTarget.getAttribute('data-room-number');
                roomInput.value = roomNumber;
                roomInput.classList.add('selected');
                this.selectedRoom = parseInt(roomNumber);
                this.hideRoomSuggestions();
                this.updatePreview();
            });
        });
    }

    hideGuestSuggestions() {
        document.getElementById('guestSuggestions').classList.remove('show');
    }

    hideRoomSuggestions() {
        document.getElementById('roomSuggestions').classList.remove('show');
    }

    handleGuestKeydown(e) {
        const suggestions = document.querySelectorAll('#guestSuggestions .suggestion-item');
        if (suggestions.length === 0) return;

        const highlighted = document.querySelector('#guestSuggestions .suggestion-item.highlighted');
        let nextIndex = 0;

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                if (highlighted) {
                    nextIndex = (Array.from(suggestions).indexOf(highlighted) + 1) % suggestions.length;
                }
                this.updateGuestHighlight(nextIndex);
                break;

            case 'ArrowUp':
                e.preventDefault();
                if (highlighted) {
                    nextIndex = (Array.from(suggestions).indexOf(highlighted) - 1 + suggestions.length) % suggestions.length;
                } else {
                    nextIndex = suggestions.length - 1;
                }
                this.updateGuestHighlight(nextIndex);
                break;

            case 'Enter':
                e.preventDefault();
                if (highlighted) {
                    const guestName = highlighted.getAttribute('data-guest-name');
                    document.getElementById('guestName').value = guestName;
                    document.getElementById('guestName').classList.add('selected');
                    this.selectedGuest = guestName;
                    this.hideGuestSuggestions();
                    this.updatePreview();
                }
                break;

            case 'Escape':
                this.hideGuestSuggestions();
                break;
        }
    }

    handleRoomKeydown(e) {
        const suggestions = document.querySelectorAll('#roomSuggestions .suggestion-item');
        if (suggestions.length === 0) return;

        const highlighted = document.querySelector('#roomSuggestions .suggestion-item.highlighted');
        let nextIndex = 0;

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                if (highlighted) {
                    nextIndex = (Array.from(suggestions).indexOf(highlighted) + 1) % suggestions.length;
                }
                this.updateRoomHighlight(nextIndex);
                break;

            case 'ArrowUp':
                e.preventDefault();
                if (highlighted) {
                    nextIndex = (Array.from(suggestions).indexOf(highlighted) - 1 + suggestions.length) % suggestions.length;
                } else {
                    nextIndex = suggestions.length - 1;
                }
                this.updateRoomHighlight(nextIndex);
                break;

            case 'Enter':
                e.preventDefault();
                if (highlighted) {
                    const roomNumber = highlighted.getAttribute('data-room-number');
                    document.getElementById('roomNumber').value = roomNumber;
                    document.getElementById('roomNumber').classList.add('selected');
                    this.selectedRoom = parseInt(roomNumber);
                    this.hideRoomSuggestions();
                    this.updatePreview();
                }
                break;

            case 'Escape':
                this.hideRoomSuggestions();
                break;
        }
    }

    updateGuestHighlight(index) {
        const suggestions = document.querySelectorAll('#guestSuggestions .suggestion-item');
        suggestions.forEach((item, i) => {
            item.classList.toggle('highlighted', i === index);
        });
    }

    updateRoomHighlight(index) {
        const suggestions = document.querySelectorAll('#roomSuggestions .suggestion-item');
        suggestions.forEach((item, i) => {
            item.classList.toggle('highlighted', i === index);
        });
    }

    showReservationPreview() {
        const previewContainer = document.getElementById('reservationPreview');
        if (!previewContainer) return;
        
        previewContainer.className = 'reservation-preview';
        previewContainer.style.display = 'none';
        this.updatePreview();
    }

    updatePreview() {
        const guestName = document.getElementById('guestName').value.trim();
        const roomNumber = document.getElementById('roomNumber').value;
        const previewContainer = document.getElementById('reservationPreview');
        
        if (!previewContainer) return;
        
        if (!guestName && !roomNumber && this.selectedDates.size === 0) {
            previewContainer.style.display = 'none';
            return;
        }
        
        let previewHTML = '<div class="preview-header">📋 Pré-visualização da Reserva</div>';
        
        if (guestName) {
            const guestExists = this.allGuests.some(g => 
                g.name.toLowerCase() === guestName.toLowerCase()
            );
            previewHTML += `
                <div class="preview-item ${guestExists ? 'available' : ''}">
                    <strong>Hóspede:</strong> ${this.escapeHtml(guestName)}
                    ${guestExists ? '✅ Existente' : '🆕 Novo (será criado)'}
                </div>
            `;
        }
        
        if (roomNumber && !isNaN(parseInt(roomNumber))) {
            const roomNum = parseInt(roomNumber);
            const room = this.allRooms.find(r => r.number === roomNum);
            previewHTML += `
                <div class="preview-item ${room ? 'available' : 'unavailable'}">
                    <strong>Quarto:</strong> ${roomNumber}
                    ${room ? '✅ Disponível' : '❌ Não encontrado'}
                </div>
            `;
        }
        
        if (this.selectedDates.size > 0) {
            const sortedDates = Array.from(this.selectedDates).sort();
            previewHTML += `
                <div class="preview-item">
                    <strong>Datas (${this.selectedDates.size}):</strong> 
                    ${sortedDates.slice(0, 3).map(d => this.formatDateForDisplay(d)).join(', ')}
                    ${sortedDates.length > 3 ? `... +${sortedDates.length - 3} mais` : ''}
                </div>
            `;
            
            if (roomNumber && !isNaN(parseInt(roomNumber))) {
                this.checkRoomAvailability(parseInt(roomNumber), this.selectedDates)
                    .then(availability => {
                        const availabilityElement = document.createElement('div');
                        availabilityElement.className = `preview-item ${availability.available ? 'available' : 'unavailable'}`;
                        availabilityElement.innerHTML = `<strong>Disponibilidade:</strong> ${availability.message}`;
                        previewContainer.appendChild(availabilityElement);
                    });
            }
        }
        
        previewContainer.innerHTML = previewHTML;
        previewContainer.style.display = 'block';
    }

    formatDateForDisplay(dateObj) {
        const date = this.parseDate(dateObj);
        return date.toLocaleDateString('pt-BR');
    }

    async checkRoomAvailability(roomNumber, dates) {
        try {
            const roomExists = this.allRooms.some(r => r.number === roomNumber);
            if (!roomExists) {
                return {
                    available: false,
                    message: `Quarto ${roomNumber} não encontrado`
                };
            }

            const dateStrings = Array.from(dates);
            let hasConflict = false;
            let conflictDate = null;

            for (const dateStr of dateStrings) {
                const reservedRooms = this.reservedDates.get(dateStr) || [];
                if (reservedRooms.includes(roomNumber)) {
                    hasConflict = true;
                    conflictDate = dateStr;
                    break;
                }
            }

            if (hasConflict) {
                return {
                    available: false,
                    message: `Quarto ${roomNumber} já está reservado para ${this.formatDateForDisplay(conflictDate)}`
                };
            }

            return {
                available: true,
                message: `Quarto ${roomNumber} disponível para as datas selecionadas`
            };

        } catch (error) {
            console.error('Erro ao verificar disponibilidade:', error);
            return {
                available: false,
                message: 'Erro ao verificar disponibilidade do quarto'
            };
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
        
        if (this.expandedDate === dateString) {
            dayElement.classList.add('expanded');
        }
        
        const dayNumberElement = document.createElement('div');
        dayNumberElement.className = 'calendar-day-number';
        dayNumberElement.textContent = dayNumber;
        dayElement.appendChild(dayNumberElement);
        
        const eventsElement = document.createElement('div');
        eventsElement.className = 'calendar-day-events';

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
            
            if (reservedRooms.length > 0 && reservedRooms.length < totalRooms) {
                const availableRooms = totalRooms - reservedRooms.length;
                const availabilityElement = document.createElement('div');
                availabilityElement.className = 'event available';
                availabilityElement.textContent = `${availableRooms} livre(s)`;
                availabilityElement.title = `${availableRooms} quarto(s) disponível(is)`;
                eventsElement.appendChild(availabilityElement);
            }
            
            const isFormVisible = document.getElementById('reservationForm').style.display === 'block';
            if (isFormVisible && !dayElement.classList.contains('past') && !dayElement.classList.contains('fully-reserved')) {
                const selectIndicator = document.createElement('div');
                selectIndicator.className = 'event available';
                selectIndicator.style.fontSize = '0.6rem';
                selectIndicator.style.marginTop = '2px';
                selectIndicator.style.background = 'var(--blue-light)';
                selectIndicator.style.color = 'white';
                eventsElement.appendChild(selectIndicator);
            }
        } else {
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
        
        const reservationsForDate = this.getReservationsForDate(dateString);
        
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

        if (reservedRooms.length > 0 && availableRooms > 0) {
            const availableTitle = document.createElement('div');
            availableTitle.className = 'event-title';
            availableTitle.textContent = 'Quartos Disponíveis:';
            availableTitle.style.fontWeight = 'bold';
            availableTitle.style.margin = '4px 0 2px 0';
            availableTitle.style.fontSize = '0.7rem';
            container.appendChild(availableTitle);
            
            const allRoomNumbers = this.allRooms.map(room => room.number);
            const occupiedRoomNumbers = reservedRooms;
            const availableRoomNumbers = allRoomNumbers.filter(roomNum => 
                !occupiedRoomNumbers.includes(roomNum)
            );

            if (availableRoomNumbers.length <= 5) {
                availableRoomNumbers.forEach(roomNum => {
                    const room = this.allRooms.find(r => r.number === roomNum);
                    const roomInfo = room ? `${room.roomTypeDescription || 'Quarto'}` : 'Quarto';
                    
                    const availableElement = document.createElement('div');
                    availableElement.className = 'event available clickable-room';
                    availableElement.setAttribute('data-room-number', roomNum);
                    availableElement.innerHTML = `
                        <strong>Quarto ${roomNum}</strong>
                        <br><small>${roomInfo}</small>
                    `;
                    availableElement.title = `Clique para selecionar quarto ${roomNum}`;
                    
                    availableElement.addEventListener('click', (e) => {
                        e.stopPropagation();
                        const roomInput = document.getElementById('roomNumber');
                        roomInput.value = roomNum;
                        roomInput.classList.add('selected');
                        this.selectedRoom = roomNum;
                        this.hideRoomSuggestions();
                        this.updatePreview();
                        
                        this.showAlert(`Quarto ${roomNum} selecionado`, 'success');
                    });
                    
                    container.appendChild(availableElement);
                });
            } else {
                const availableElement = document.createElement('div');
                availableElement.className = 'event available';
                availableElement.textContent = `${availableRooms} quartos disponíveis`;
                availableElement.title = `Clique para ver a lista completa`;
                
                availableElement.addEventListener('click', (e) => {
                    e.stopPropagation();
                    this.showAllAvailableRooms(dateString, availableRoomNumbers);
                });
                
                container.appendChild(availableElement);
            }
        }
    }

    showAllAvailableRooms(dateString, availableRoomNumbers) {
        const modal = document.getElementById('availableRoomsModal');
        const listContainer = document.getElementById('availableRoomsList');
        
        const roomsHTML = availableRoomNumbers.map(roomNum => {
            const room = this.allRooms.find(r => r.number === roomNum);
            return `
                <div class="available-room-item" data-room-number="${roomNum}">
                    <div class="room-info">
                        <div>
                            <div class="room-number">Quarto ${roomNum}</div>
                            <div class="room-type">${room?.roomTypeDescription || 'Tipo não especificado'}</div>
                        </div>
                        <div class="room-price">R$ ${room?.price ? parseFloat(room.price).toFixed(2) : '0,00'}</div>
                    </div>
                </div>
            `;
        }).join('');
        
        listContainer.innerHTML = roomsHTML;
        modal.style.display = 'block';
        
        listContainer.querySelectorAll('.available-room-item').forEach(item => {
            item.addEventListener('click', () => {
                const roomNum = item.getAttribute('data-room-number');
                const roomInput = document.getElementById('roomNumber');
                roomInput.value = roomNum;
                roomInput.classList.add('selected');
                this.selectedRoom = parseInt(roomNum);
                this.updatePreview();
                this.closeAvailableRoomsModal();
                
                this.showAlert(`Quarto ${roomNum} selecionado`, 'success');
            });
        });
    }

    closeAvailableRoomsModal() {
        document.getElementById('availableRoomsModal').style.display = 'none';
    }

    toggleDateExpansion(date) {
        const dateString = this.formatDate(date);
        
        const isReservationFormVisible = document.getElementById('reservationForm').style.display === 'block';
        
        if (isReservationFormVisible) {
            this.toggleDateSelection(date);
        } else {
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
        
        if (date < today) {
            this.showAlert('Não é possível selecionar datas passadas', 'error');
            return;
        }
        
        const reservedRooms = this.reservedDates.get(dateString) || [];
        const totalRooms = this.allRooms.length;
        
        if (reservedRooms.length >= totalRooms) {
            this.showAlert('Esta data está completamente ocupada', 'error');
            return;
        }
        
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
                                <i class="fas fa-cog"></i> Gerenciar
                            </button>
                            <button class="btn btn-sm btn-primary" onclick="calendarSystem.generateAndDownloadVoucher(${reservation.id})" title="Baixar Voucher">
                                <i class="fas fa-download"></i> Voucher
                            </button>
                            ${reservation.reserveStatus === 'CONFIRMED' ? `
                                <button class="btn btn-sm btn-success" onclick="calendarSystem.performCheckIn(${reservation.id})">
                                    <i class="fas fa-sign-in-alt"></i> Check-in
                                </button>
                                <button class="btn btn-sm btn-danger" onclick="calendarSystem.cancelReservationById(${reservation.id})">
                                    <i class="fas fa-times"></i> Cancelar
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
        
        reservationsCard.style.display = 'block';
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
        const dateObj = this.parseDate(date);
        const year = dateObj.getFullYear();
        const month = String(dateObj.getMonth() + 1).padStart(2, '0');
        const day = String(dateObj.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    parseDate(dateObj) {
        if (!dateObj) {
            return new Date();
        }
        
        if (dateObj instanceof Date) {
            return dateObj;
        }
        
        if (typeof dateObj === 'string') {
            if (dateObj.includes('T')) {
                return new Date(dateObj);
            }
            
            try {
                const parts = dateObj.split('-');
                if (parts.length === 3) {
                    return new Date(parts[0], parts[1] - 1, parts[2]);
                }
                return new Date(dateObj);
            } catch (error) {
                console.error('Erro ao fazer parse da string:', error);
                return new Date();
            }
        }
        
        if (typeof dateObj === 'object') {
            if (dateObj.year && dateObj.month && dateObj.day) {
                return new Date(dateObj.year, dateObj.month - 1, dateObj.day);
            }
            
            if (dateObj.$date || dateObj.iso) {
                const dateStr = dateObj.$date || dateObj.iso;
                return new Date(dateStr);
            }
            
            const dateStr = String(dateObj);
            if (dateStr.match(/\d{4}-\d{2}-\d{2}/)) {
                return this.parseDate(dateStr);
            }
        }
        
        return new Date();
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
                this.updatePreview();
            });
            
            selectedDatesContainer.appendChild(dateTag);
        });
        
        this.updatePreview();
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
            this.expandedDate = null;
        } else {
            form.style.display = 'block';
            this.selectedDates.clear();
            this.expandedDate = null;
            this.hideReservationsList();
        }
        
        this.updateSelectedDatesDisplay();
        this.loadCalendar();
    }

    cancelReservation() {
        document.getElementById('reservationForm').style.display = 'none';
        document.getElementById('reserveForm').reset();
        document.getElementById('guestName').classList.remove('selected');
        document.getElementById('roomNumber').classList.remove('selected');
        this.selectedGuest = null;
        this.selectedRoom = null;
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
                    const dateStr = this.extractDateFromLocalDate(date);
                    if (dateStr) {
                        if (!this.reservedDates.has(dateStr)) {
                            this.reservedDates.set(dateStr, []);
                        }
                        if (roomNumber !== 'N/A') {
                            this.reservedDates.get(dateStr).push(roomNumber);
                        }
                    }
                });
            }
        });
    }

    extractDateFromLocalDate(dateObj) {
        if (dateObj && typeof dateObj === 'object') {
            if (dateObj.year && dateObj.month && dateObj.day) {
                return `${dateObj.year}-${String(dateObj.month).padStart(2, '0')}-${String(dateObj.day).padStart(2, '0')}`;
            }
            
            if (dateObj.$date) {
                return dateObj.$date.substring(0, 10);
            }
            
            if (dateObj.iso) {
                return dateObj.iso.substring(0, 10);
            }
            
            if (dateObj._year || dateObj._month || dateObj._day) {
                const year = dateObj._year || dateObj.year;
                const month = dateObj._month || dateObj.month;
                const day = dateObj._day || dateObj.day;
                return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
            }
        }
        
        if (typeof dateObj === 'string') {
            return dateObj;
        }
        
        return String(dateObj);
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
        
        const guestName = document.getElementById('guestName').value.trim();
        const roomInput = document.getElementById('roomNumber').value;
        const roomNumber = parseInt(roomInput);
        
        if (this.selectedDates.size === 0) {
            this.showAlert('Selecione pelo menos uma data para reservar', 'error');
            return;
        }
        
        if (!guestName || !roomNumber || isNaN(roomNumber)) {
            this.showAlert('Preencha todos os campos obrigatórios', 'error');
            return;
        }
        
        const datesArray = Array.from(this.selectedDates).map(dateStr => {
            const date = this.parseDate(dateStr);
            return this.formatDate(date);
        });
        
        const roomExists = this.allRooms.some(r => r.number === roomNumber);
        if (!roomExists) {
            this.showAlert(`Quarto ${roomNumber} não encontrado`, 'error');
            return;
        }
        
        const availability = await this.checkRoomAvailability(roomNumber, this.selectedDates);
        if (!availability.available) {
            this.showAlert(availability.message, 'error');
            return;
        }
        
        const reserveBtn = document.getElementById('reserveBtn');
        reserveBtn.disabled = true;
        reserveBtn.innerHTML = '<div class="loading"></div> Reservando...';
        
        try {
            const reservationData = {
                dates: datesArray,
                guestName: guestName,
                roomNumber: roomNumber
            };
            
            console.log('Tentando criar reserva com /insert:', reservationData);
            
            let response = await this.makeAuthenticatedRequest(`${this.baseUrl}/insert`, {
                method: 'POST',
                body: JSON.stringify(reservationData)
            });
            
            if (response.status === 404) {
                console.log('Hóspede não encontrado, tentando com /create-with-guest');
                
                const newGuestData = {
                    guests: [{
                        name: guestName,
                        rg: "A ser preenchido",
                        phone: "Não informado",
                        email: ""
                    }],
                    dates: datesArray,
                    roomNumber: roomNumber
                };
                
                response = await this.makeAuthenticatedRequest(`${this.baseUrl}/create-with-guest`, {
                    method: 'POST',
                    body: JSON.stringify(newGuestData)
                });
                
                if (response.ok) {
                    await this.loadGuests();
                }
            }
            
            if (response.ok) {
                const reserve = await response.json();
                this.showAlert('Reserva criada com sucesso!', 'success');
                this.cancelReservation();
                this.loadReservations();
                
                Swal.fire({
                    title: '✅ Reserva Criada!',
                    html: `
                        <div style="text-align: left;">
                            <p><strong>Reserva #${reserve.id}</strong></p>
                            <p><strong>Status:</strong> ${this.getStatusText(reserve.reserveStatus)}</p>
                            <p><strong>Hóspede:</strong> ${guestName}</p>
                            <p><strong>Quarto:</strong> ${roomNumber}</p>
                            <p><strong>Datas:</strong> ${datesArray.sort().map(d => this.formatDateForDisplay(d)).join(', ')}</p>
                        </div>
                    `,
                    icon: 'success',
                    confirmButtonText: 'OK'
                });
                
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
        
        // Obter informações do hóspede
        const guestName = this.getGuestName(reservation);
        const guestPhone = reservation.guest && reservation.guest.length > 0 ? 
            (reservation.guest[0].phone || '(00) 00000-0000') : '(00) 00000-0000';
        const guestRgCpf = reservation.guest && reservation.guest.length > 0 ? 
            (reservation.guest[0].rg || 'XXXX-XXXX') : 'XXXX-XXXX';
        
        // Calcular valor total usando o método do backend ou calcular localmente
        const totalValue = reservation.calculateTotalValue ? 
            parseFloat(reservation.calculateTotalValue()).toFixed(2) :
            this.calculateReservationTotal(reservation);
        
        const html = `
            <div>
                <div class="quick-stats">
                    <div class="stat-card">
                        <div class="stat-number">#${reservation.id.toString().padStart(6, '0')}</div>
                        <div class="stat-label">Código Reserva</div>
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

                <!-- SEÇÃO DO VOUCHER -->
                <div class="management-section voucher-section">
                    <h4 class="section-title">📄 Voucher de Hospedagem</h4>
                    <div class="voucher-preview">
                        <div class="voucher-preview-content">
                            <h3>VOUCHER DE HOSPEDAGEM</h3>
                            <h4>COMPROVANTE DE RESERVA</h4>
                            
                            <div class="reservation-code">
                                CÓDIGO DA RESERVA: ${reservation.id.toString().padStart(6, '0')}
                            </div>
                            
                            <div class="hotel-info">
                                <div>
                                    <p><strong>Endereço:</strong> QNM 16 Conjunto B</p>
                                    <p><strong>Contato:</strong> (61) 99999-9999</p>
                                </div>
                                <div>
                                    <p><strong>Nome do Estabelecimento:</strong> Elô AP</p>
                                    <p><strong>Responsável:</strong> Elô</p>
                                </div>
                            </div>
                            
                            <div style="margin: 20px 0; padding: 15px; background: #f0f7ff; border-radius: 5px;">
                                <p><strong>Check-in:</strong> ${this.getFirstReservationDate(reservation)}</p>
                                <p><strong>Check-out:</strong> ${this.getLastReservationDate(reservation)}</p>
                            </div>
                            
                            <div class="guest-info">
                                <h4>Dados do Hóspede:</h4>
                                <div class="guest-details">
                                    <div>
                                        <p><strong>Nome do hóspede:</strong> ${guestName}</p>
                                        <p><strong>Telefone:</strong> ${guestPhone}</p>
                                    </div>
                                    <div>
                                        <p><strong>RG/CPF:</strong> ${guestRgCpf}</p>
                                        <div class="value-total">
                                            <strong>VALOR TOTAL:</strong> ${this.formatCurrency(parseFloat(totalValue))}
                                        </div>
                                        <div class="value-breakdown">
                                            ${reservation.numberOfDays ? `
                                                <div class="value-breakdown-item">
                                                    <span>Diária base:</span>
                                                    <span>${this.formatCurrency(reservation.initialValue || 0)} x ${reservation.numberOfDays} dias</span>
                                                </div>
                                            ` : ''}
                                            ${reservation.numberOfExtraGuests > 0 ? `
                                                <div class="value-breakdown-item">
                                                    <span>Taxa hóspedes extras:</span>
                                                    <span>${this.formatCurrency(reservation.extraGuestFee || 20)} x ${reservation.numberOfExtraGuests} hóspede(s) x ${reservation.numberOfDays} dias</span>
                                                </div>
                                            ` : ''}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="print-controls">
                        <button class="btn btn-primary btn-voucher" onclick="calendarSystem.generateAndDownloadVoucher(${reservation.id})" id="downloadVoucherBtn">
                            <i class="fas fa-download"></i> Baixar Voucher (PDF)
                        </button>
                    </div>
                </div>
                <!-- FIM DA SEÇÃO DO VOUCHER -->

                <div class="management-section">
                    <h4 class="section-title">📊 Status da Reserva</h4>
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <span class="status-badge status-${reservation.reserveStatus ? reservation.reserveStatus.toLowerCase() : 'unknown'}">
                                ${this.getStatusText(reservation.reserveStatus) || 'N/A'}
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

                <div class="management-section">
                    <h4 class="section-title">⚡ Ações Rápidas</h4>
                    <div class="action-grid">
                        <div class="action-card" onclick="calendarSystem.openAddDatesModal(${reservation.id})">
                            <div class="action-icon">📅</div>
                            <div class="action-title">Adicionar Datas</div>
                            <div class="action-description">Incluir novas datas na reserva</div>
                        </div>
                        <div class="action-card" onclick="calendarSystem.focusAddGuest(${reservation.id})">
                            <div class="action-icon">👥</div>
                            <div class="action-title">Adicionar Hóspede</div>
                            <div class="action-description">Incluir novo hóspede</div>
                        </div>
                    </div>
                </div>

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

                <div class="management-section">
                    <h4 class="section-title">👥 Hóspedes</h4>
                    <div class="guest-list" id="currentGuestList">
                        ${this.formatGuestList(reservation.guest, reservation.id)}
                    </div>
                    <div class="form-group" style="margin-top: 1rem;">
                        <label class="form-label">Adicionar Novo Hóspede</label>
                        <div class="autocomplete-container">
                            <input 
                                type="text" 
                                id="newGuestName" 
                                class="form-control" 
                                placeholder="Digite o nome do hóspede"
                                autocomplete="off"
                                style="flex: 1;"
                            >
                            <div id="newGuestSuggestions" class="autocomplete-suggestions"></div>
                        </div>
                        <div class="button-group">
                            <button class="btn btn-success btn-sm" onclick="calendarSystem.addGuest(${reservation.id})" style="margin-top: 0.5rem;">
                                <i class="fas fa-plus"></i> Adicionar Hóspede
                            </button>
                            <button class="btn btn-primary btn-sm" onclick="calendarSystem.openCreateGuestModal()" style="margin-top: 0.5rem; margin-left: 0.5rem;">
                                <i class="fas fa-user-plus"></i> Criar Novo Hóspede
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        container.innerHTML = html;
        document.getElementById('manageModal').style.display = 'block';
        
        setTimeout(() => {
            this.initManageModalAutocomplete();
        }, 100);
    }

    calculateReservationTotal(reservation) {
        // Cálculo local se o backend não fornecer calculateTotalValue
        const initialValue = reservation.initialValue || 0;
        const numberOfDays = reservation.reservedDays ? 
            (Array.isArray(reservation.reservedDays) ? reservation.reservedDays.length : reservation.reservedDays.size) : 0;
        const extraGuestFee = reservation.extraGuestFee || 20;
        const numberOfExtraGuests = Math.max(0, (reservation.guest ? 
            (Array.isArray(reservation.guest) ? reservation.guest.length : reservation.guest.size) : 0) - 1);
        
        // Calcular valor total: (valor inicial × dias) + (taxa extra × hóspedes extras × dias)
        const baseTotal = initialValue * numberOfDays;
        const extraTotal = extraGuestFee * numberOfExtraGuests * numberOfDays;
        
        return (baseTotal + extraTotal).toFixed(2);
    }

    formatCurrency(value) {
        return new Intl.NumberFormat('pt-BR', {
            style: 'currency',
            currency: 'BRL'
        }).format(value || 0);
    }

    getFirstReservationDate(reservation) {
        if (reservation.reservedDays && reservation.reservedDays.length > 0) {
            const dates = Array.isArray(reservation.reservedDays) ? reservation.reservedDays : Array.from(reservation.reservedDays);
            const sortedDates = dates.sort();
            return this.formatDateForDisplay(sortedDates[0]);
        }
        return 'N/A';
    }

    getLastReservationDate(reservation) {
        if (reservation.reservedDays && reservation.reservedDays.length > 0) {
            const dates = Array.isArray(reservation.reservedDays) ? reservation.reservedDays : Array.from(reservation.reservedDays);
            const sortedDates = dates.sort();
            return this.formatDateForDisplay(sortedDates[sortedDates.length - 1]);
        }
        return 'N/A';
    }

    async generateAndDownloadVoucher(reservationId) {
        try {
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/find/${reservationId}`);
            
            if (response.ok) {
                const reservation = await response.json();
                
                // Desabilitar botão e mostrar loading
                const downloadBtn = document.getElementById('downloadVoucherBtn');
                if (downloadBtn) {
                    const originalText = downloadBtn.innerHTML;
                    downloadBtn.disabled = true;
                    downloadBtn.innerHTML = '<div class="loading"></div> Gerando PDF...';
                    
                    // Restaurar botão após 3 segundos (caso o PDF falhe)
                    setTimeout(() => {
                        if (downloadBtn.disabled) {
                            downloadBtn.disabled = false;
                            downloadBtn.innerHTML = originalText;
                        }
                    }, 3000);
                }
                
                // Mostrar alerta de processamento
                Swal.fire({
                    title: 'Gerando PDF...',
                    text: 'Por favor, aguarde enquanto o voucher é gerado.',
                    allowOutsideClick: false,
                    didOpen: () => {
                        Swal.showLoading();
                    }
                });
                
                // Gerar PDF usando HTML2Canvas (melhor para manter layout)
                await this.generatePDFFromHTML(reservation);
                
                // Fechar alerta
                Swal.close();
                
                // Restaurar botão
                if (downloadBtn) {
                    downloadBtn.disabled = false;
                    downloadBtn.innerHTML = '<i class="fas fa-download"></i> Baixar Voucher (PDF)';
                }
                
                this.showAlert('Voucher baixado com sucesso!', 'success');
                
            } else {
                throw new Error('Erro ao carregar dados da reserva');
            }
        } catch (error) {
            console.error('Erro ao gerar voucher:', error);
            Swal.close();
            
            // Restaurar botão em caso de erro
            const downloadBtn = document.getElementById('downloadVoucherBtn');
            if (downloadBtn) {
                downloadBtn.disabled = false;
                downloadBtn.innerHTML = '<i class="fas fa-download"></i> Baixar Voucher (PDF)';
            }
            
            this.showAlert('Erro ao gerar voucher: ' + error.message, 'error');
        }
    }

    async generatePDFFromHTML(reservation) {
        return new Promise(async (resolve, reject) => {
            try {
                // Verificar se as bibliotecas estão disponíveis
                if (typeof html2canvas === 'undefined') {
                    throw new Error('Biblioteca html2canvas não carregada. Adicione o script no HTML.');
                }
                
                if (typeof jspdf === 'undefined') {
                    throw new Error('Biblioteca jsPDF não carregada. Adicione o script no HTML.');
                }
                
                // Criar elemento HTML temporário com estilo otimizado para PDF
                const tempDiv = document.createElement('div');
                tempDiv.style.position = 'absolute';
                tempDiv.style.left = '-9999px';
                tempDiv.style.top = '0';
                tempDiv.style.width = '794px'; // A4 em pixels (210mm)
                tempDiv.style.minHeight = '1123px'; // Altura A4
                tempDiv.style.padding = '40px';
                tempDiv.style.backgroundColor = 'white';
                tempDiv.style.fontFamily = 'Arial, Helvetica, sans-serif';
                tempDiv.style.boxSizing = 'border-box';
                
                // Gerar conteúdo HTML otimizado para PDF
                tempDiv.innerHTML = this.generatePDFVoucherHTML(reservation);
                document.body.appendChild(tempDiv);
                
                // Aguardar renderização
                await new Promise(resolve => setTimeout(resolve, 500));
                
                // Converter para canvas com alta qualidade
                const canvas = await html2canvas(tempDiv, {
                    scale: 2, // Maior qualidade
                    useCORS: true,
                    logging: false,
                    backgroundColor: '#ffffff',
                    allowTaint: true,
                    removeContainer: true
                });
                
                // Limpar elemento temporário
                document.body.removeChild(tempDiv);
                
                // Converter canvas para PDF
                const { jsPDF } = window.jspdf;
                const doc = new jsPDF({
                    orientation: 'portrait',
                    unit: 'px',
                    format: 'a4'
                });
                
                const imgData = canvas.toDataURL('image/jpeg', 1.0);
                const pdfWidth = doc.internal.pageSize.getWidth();
                const pdfHeight = doc.internal.pageSize.getHeight();
                
                // Adicionar imagem ao PDF
                doc.addImage(imgData, 'JPEG', 0, 0, pdfWidth, pdfHeight);
                
                // Nome do arquivo
                const guestName = this.getGuestName(reservation);
                const safeFileName = guestName.replace(/[^a-zA-Z0-9]/g, '_');
                const fileName = `Voucher_Reserva_${reservation.id}_${safeFileName}.pdf`;
                
                // Baixar PDF
                doc.save(fileName);
                
                resolve();
                
            } catch (error) {
                console.error('Erro no generatePDFFromHTML:', error);
                
                // Fallback: tentar método simples
                try {
                    await this.generateSimplePDFVoucher(reservation);
                    resolve();
                } catch (fallbackError) {
                    reject(fallbackError);
                }
            }
        });
    }

    generatePDFVoucherHTML(reservation) {
        const firstDate = this.getFirstReservationDate(reservation);
        const lastDate = this.getLastReservationDate(reservation);
        const guestName = this.getGuestName(reservation);
        const guestPhone = reservation.guest && reservation.guest.length > 0 ? 
            (reservation.guest[0].phone || '(00) 00000-0000') : '(00) 00000-0000';
        const guestRgCpf = reservation.guest && reservation.guest.length > 0 ? 
            (reservation.guest[0].rg || 'XXXX-XXXX') : 'XXXX-XXXX';
        const roomNumber = this.getRoomNumber(reservation);
        
        // Calcular valor total
        const totalValue = reservation.calculateTotalValue ? 
            parseFloat(reservation.calculateTotalValue()).toFixed(2) :
            this.calculateReservationTotal(reservation);
        
        return `
            <div style="width: 100%; height: 100%; background: white; color: #333;">
                <!-- Cabeçalho -->
                <div style="text-align: center; margin-bottom: 30px; padding-bottom: 20px; border-bottom: 3px solid #ff9800;">
                    <h1 style="color: #ff9800; font-size: 28px; margin: 0; text-transform: uppercase;">VOUCHER DE HOSPEDAGEM</h1>
                    <h2 style="color: #ff5722; font-size: 18px; margin: 5px 0 0 0; font-weight: normal;">COMPROVANTE DE RESERVA</h2>
                </div>
                
                <!-- Código da Reserva -->
                <div style="background: #fff3cd; padding: 15px; text-align: center; font-size: 20px; font-weight: bold; margin: 20px 0; border: 2px dashed #ff9800; border-radius: 5px;">
                    CÓDIGO DA RESERVA: ${reservation.id.toString().padStart(6, '0')}
                </div>
                
                <!-- Informações do Hotel -->
                <div style="display: flex; justify-content: space-between; margin-bottom: 30px; padding: 20px; background: #f9f9f9; border-radius: 5px;">
                    <div>
                        <p style="margin: 8px 0;"><strong>Endereço:</strong> QNM 16 Conjunto B</p>
                        <p style="margin: 8px 0;"><strong>Contato:</strong> (61) 99999-9999</p>
                    </div>
                    <div>
                        <p style="margin: 8px 0;"><strong>Nome do Estabelecimento:</strong> Elô AP</p>
                        <p style="margin: 8px 0;"><strong>Responsável:</strong> Elô</p>
                    </div>
                </div>
                
                <!-- Datas da Hospedagem -->
                <div style="background: #e8f5e9; padding: 20px; margin: 20px 0; border-radius: 5px; border-left: 5px solid #4caf50;">
                    <h3 style="color: #2e7d32; margin-top: 0;">Datas da Hospedagem:</h3>
                    <p style="margin: 8px 0;"><strong>Check-in:</strong> ${firstDate}</p>
                    <p style="margin: 8px 0;"><strong>Check-out:</strong> ${lastDate}</p>
                    <p style="margin: 8px 0;"><strong>Quarto:</strong> ${roomNumber}</p>
                </div>
                
                <!-- Dados do Hóspede -->
                <div style="background: #e3f2fd; padding: 25px; margin: 25px 0; border-radius: 5px; border-left: 5px solid #2196f3;">
                    <h3 style="color: #1565c0; margin-top: 0;">Dados do Hóspede:</h3>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px;">
                        <div>
                            <p style="margin: 8px 0;"><strong>Nome do hóspede:</strong> ${guestName}</p>
                            <p style="margin: 8px 0;"><strong>Telefone:</strong> ${guestPhone}</p>
                        </div>
                        <div>
                            <p style="margin: 8px 0;"><strong>RG/CPF:</strong> ${guestRgCpf}</p>
                        </div>
                    </div>
                    
                    <!-- Detalhamento do Valor -->
                    <div style="margin-top: 15px; padding: 15px; background: white; border-radius: 5px; border: 1px solid #ddd;">
                        <h4 style="margin-top: 0;">Detalhamento do Valor:</h4>
                        ${reservation.numberOfDays ? `
                            <div style="display: flex; justify-content: space-between; margin-bottom: 8px; padding: 5px 0; border-bottom: 1px solid #eee;">
                                <span>Diária base:</span>
                                <span>${this.formatCurrency(reservation.initialValue || 0)} x ${reservation.numberOfDays} dias</span>
                            </div>
                        ` : ''}
                        ${reservation.numberOfExtraGuests > 0 ? `
                            <div style="display: flex; justify-content: space-between; margin-bottom: 8px; padding: 5px 0; border-bottom: 1px solid #eee;">
                                <span>Taxa hóspedes extras:</span>
                                <span>${this.formatCurrency(reservation.extraGuestFee || 20)} x ${reservation.numberOfExtraGuests} hóspede(s) x ${reservation.numberOfDays} dias</span>
                            </div>
                        ` : ''}
                        <div style="font-weight: bold; font-size: 1.2em; color: #2196F3; margin-top: 15px; padding-top: 15px; border-top: 2px solid #ddd; text-align: right;">
                            <strong>VALOR TOTAL:</strong> ${this.formatCurrency(parseFloat(totalValue))}
                        </div>
                    </div>
                </div>
                
                <!-- Termos e Condições -->
                <div style="padding: 20px; background: #fffde7; border-radius: 5px; border: 1px solid #ffd54f; margin-top: 30px; font-size: 13px; line-height: 1.6;">
                    <h3 style="color: #ff9800; margin-top: 0;">Termos e Condições:</h3>
                    <p>Seja bem-vindo ao nosso espaço de acomodação! Esperamos que você esteja bem e que se sinta em casa. Vamos passar aqui apenas algumas informações importantes para que sua estádia seja agradável e tranquila.</p>
                    <p>Seu quarto possuí equipamentos funcionais que estão à sua disposição, assim como talheres, utensílios e eletrodomésticos (Geladeira, Airfryer, TV, etc.). Pedimos que utilize com zelo, pois danos, ou objetos quebrados podem ser cobrados ao fim da estádia.</p>
                    <p>Caso o hóspede perca a chave ele deve ressarcir o valor da(s) cópia(s).</p>
                    <p>Nossa política de devolução respeita à vigente nos aplicativos de hospedagens, para reservas feitas nos mesmos, ou devolução integral do valor pago para desistências avisadas com até 5 dias de antecedência, e devolução parcial para desistências avisadas com menos de 5 dias (Neste caso o valor deve ser consultado com o Responsável). Para hóspedes que não compareçam à estádia e não avisem com prazo de até 24 horas do dia marcado para o check in, não terá direito à devolução.</p>
                    <p>Essas são as nossas regras, e desejamos uma boa hospedagem! ♥️</p>
                </div>
                
                <!-- Assinatura -->
                <div style="margin-top: 50px; padding-top: 20px; border-top: 2px solid #333; text-align: right;">
                    <p style="margin: 10px 0;">_________________________________________</p>
                    <p style="margin: 10px 0;"><strong>Responsável:</strong> Elô</p>
                    <p style="margin: 10px 0;"><strong>Data:</strong> ${new Date().toLocaleDateString('pt-BR')}</p>
                </div>
                
                <!-- Rodapé -->
                <div style="margin-top: 20px; padding-top: 10px; border-top: 1px solid #ccc; text-align: center; font-size: 10px; color: #666;">
                    Documento gerado automaticamente pelo Sistema Interactive Edge
                </div>
            </div>
        `;
    }

    // Método de fallback simples
    async generateSimplePDFVoucher(reservation) {
        return new Promise((resolve, reject) => {
            try {
                const { jsPDF } = window.jspdf;
                const doc = new jsPDF();
                
                // Dados básicos
                const guestName = this.getGuestName(reservation);
                const totalValue = reservation.calculateTotalValue ? 
                    parseFloat(reservation.calculateTotalValue()).toFixed(2) :
                    this.calculateReservationTotal(reservation);
                
                // Configuração
                doc.setFont('helvetica');
                
                // Cabeçalho
                doc.setFontSize(20);
                doc.setTextColor(255, 152, 0); // Laranja
                doc.text('VOUCHER DE HOSPEDAGEM', 105, 20, { align: 'center' });
                
                doc.setFontSize(14);
                doc.setTextColor(255, 87, 34); // Laranja escuro
                doc.text('COMPROVANTE DE RESERVA', 105, 30, { align: 'center' });
                
                // Linha divisória
                doc.setDrawColor(255, 152, 0);
                doc.setLineWidth(0.5);
                doc.line(20, 35, 190, 35);
                
                // Código da Reserva
                doc.setFontSize(16);
                doc.setTextColor(102, 60, 0); // Marrom
                doc.text(`CÓDIGO: ${reservation.id.toString().padStart(6, '0')}`, 105, 45, { align: 'center' });
                
                // Informações do Hotel
                doc.setFontSize(11);
                doc.setTextColor(0, 0, 0);
                doc.setFont('helvetica', 'bold');
                doc.text('ELÔ AP - HOSPEDAGEM', 20, 60);
                doc.setFont('helvetica', 'normal');
                doc.text('Endereço: QNM 16 Conjunto B', 20, 68);
                doc.text('Contato: (61) 99999-9999', 20, 76);
                doc.text('Responsável: Elô', 20, 84);
                
                // Datas
                doc.setFont('helvetica', 'bold');
                doc.text('DATAS DA HOSPEDAGEM:', 20, 95);
                doc.setFont('helvetica', 'normal');
                doc.text(`Check-in: ${this.getFirstReservationDate(reservation)}`, 20, 103);
                doc.text(`Check-out: ${this.getLastReservationDate(reservation)}`, 20, 111);
                doc.text(`Quarto: ${this.getRoomNumber(reservation)}`, 20, 119);
                
                // Dados do Hóspede
                doc.setFont('helvetica', 'bold');
                doc.text('DADOS DO HÓSPEDE:', 20, 130);
                doc.setFont('helvetica', 'normal');
                doc.text(`Nome: ${guestName}`, 20, 138);
                doc.text(`Telefone: ${reservation.guest && reservation.guest.length > 0 ? (reservation.guest[0].phone || 'Não informado') : 'Não informado'}`, 20, 146);
                doc.text(`RG/CPF: ${reservation.guest && reservation.guest.length > 0 ? (reservation.guest[0].rg || 'Não informado') : 'Não informado'}`, 20, 154);
                
                // Valor
                doc.setFont('helvetica', 'bold');
                doc.text('VALOR TOTAL:', 20, 165);
                doc.setFontSize(14);
                doc.setTextColor(33, 150, 243); // Azul
                doc.text(`R$ ${totalValue}`, 60, 165);
                
                // Detalhamento do Valor
                doc.setFontSize(9);
                doc.setTextColor(0, 0, 0);
                doc.setFont('helvetica', 'normal');
                
                let yPos = 175;
                if (reservation.numberOfDays) {
                    const dailyValue = reservation.initialValue || 0;
                    doc.text(`Diária base: R$ ${dailyValue.toFixed(2)} x ${reservation.numberOfDays} dias`, 20, yPos);
                    yPos += 5;
                }
                
                if (reservation.numberOfExtraGuests > 0) {
                    const extraFee = reservation.extraGuestFee || 20;
                    doc.text(`Taxa hóspedes extras: R$ ${extraFee.toFixed(2)} x ${reservation.numberOfExtraGuests} hóspede(s) x ${reservation.numberOfDays} dias`, 20, yPos);
                    yPos += 5;
                }
                
                // Assinatura
                doc.setDrawColor(0, 0, 0);
                doc.setLineWidth(0.5);
                doc.line(120, 250, 190, 250);
                
                doc.setFontSize(10);
                doc.text('Responsável: Elô', 120, 260);
                doc.text(`Data: ${new Date().toLocaleDateString('pt-BR')}`, 120, 267);
                
                // Rodapé
                doc.setFontSize(7);
                doc.setTextColor(128, 128, 128);
                doc.text('Documento gerado automaticamente pelo Sistema Interactive Edge', 105, 285, { align: 'center' });
                
                // Baixar
                doc.save(`Voucher_${reservation.id}.pdf`);
                
                resolve();
                
            } catch (error) {
                reject(error);
            }
        });
    }

    initManageModalAutocomplete() {
        const newGuestInput = document.getElementById('newGuestName');
        const suggestionsContainer = document.getElementById('newGuestSuggestions');
        
        if (!newGuestInput || !suggestionsContainer) return;
        
        newGuestInput.addEventListener('input', (e) => this.searchNewGuests(e.target.value));
        newGuestInput.addEventListener('keydown', (e) => this.handleNewGuestKeydown(e));
        newGuestInput.addEventListener('focus', () => {
            if (newGuestInput.value.length >= 2) {
                this.showNewGuestSuggestions(newGuestInput.value);
            }
        });
        newGuestInput.addEventListener('blur', () => {
            setTimeout(() => this.hideNewGuestSuggestions(), 200);
        });
    }

    searchNewGuests(query) {
        clearTimeout(this.newGuestSearchTimeout);
        
        this.newGuestSearchTimeout = setTimeout(() => {
            if (!query || query.length < 2) {
                this.hideNewGuestSuggestions();
                return;
            }
            this.showNewGuestSuggestions(query);
        }, 300);
    }

    showNewGuestSuggestions(query) {
        const suggestionsContainer = document.getElementById('newGuestSuggestions');
        const input = document.getElementById('newGuestName');
        
        if (!this.allGuests || this.allGuests.length === 0) {
            suggestionsContainer.innerHTML = '<div class="suggestion-item">Carregando hóspedes...</div>';
            suggestionsContainer.classList.add('show');
            return;
        }
        
        const lowerQuery = query.toLowerCase();
        const filteredGuests = this.allGuests.filter(guest => 
            guest.name && guest.name.toLowerCase().includes(lowerQuery)
        ).slice(0, 8);
        
        if (filteredGuests.length === 0) {
            suggestionsContainer.innerHTML = `
                <div class="suggestion-item">
                    <div><i class="fas fa-exclamation-circle"></i> Nenhum hóspede encontrado</div>
                    <div class="suggestion-details">Clique no botão "Criar Novo Hóspede"</div>
                </div>
            `;
            suggestionsContainer.classList.add('show');
            return;
        }
        
        const suggestionsHTML = filteredGuests.map((guest, index) => `
            <div class="suggestion-item ${index === 0 ? 'highlighted' : ''}" 
                 data-guest-id="${guest.id}"
                 data-guest-name="${this.escapeHtml(guest.name)}">
                <div><strong>${this.escapeHtml(guest.name)}</strong></div>
                ${guest.rg ? `<div class="suggestion-details">RG: ${this.escapeHtml(guest.rg)}</div>` : ''}
                ${guest.phone ? `<div class="suggestion-details">Tel: ${this.escapeHtml(guest.phone)}</div>` : ''}
            </div>
        `).join('');
        
        suggestionsContainer.innerHTML = suggestionsHTML;
        suggestionsContainer.classList.add('show');
        
        suggestionsContainer.querySelectorAll('.suggestion-item').forEach(item => {
            item.addEventListener('click', (e) => {
                const guestName = e.currentTarget.getAttribute('data-guest-name');
                input.value = guestName;
                this.hideNewGuestSuggestions();
            });
        });
    }

    hideNewGuestSuggestions() {
        const suggestionsContainer = document.getElementById('newGuestSuggestions');
        suggestionsContainer.classList.remove('show');
    }

    handleNewGuestKeydown(e) {
        const suggestions = document.querySelectorAll('#newGuestSuggestions .suggestion-item');
        if (suggestions.length === 0) return;

        const highlighted = document.querySelector('#newGuestSuggestions .suggestion-item.highlighted');
        let nextIndex = 0;

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                if (highlighted) {
                    nextIndex = (Array.from(suggestions).indexOf(highlighted) + 1) % suggestions.length;
                }
                this.updateNewGuestHighlight(nextIndex);
                break;

            case 'ArrowUp':
                e.preventDefault();
                if (highlighted) {
                    nextIndex = (Array.from(suggestions).indexOf(highlighted) - 1 + suggestions.length) % suggestions.length;
                } else {
                    nextIndex = suggestions.length - 1;
                }
                this.updateNewGuestHighlight(nextIndex);
                break;

            case 'Enter':
                e.preventDefault();
                if (highlighted) {
                    const guestName = highlighted.getAttribute('data-guest-name');
                    document.getElementById('newGuestName').value = guestName;
                    this.hideNewGuestSuggestions();
                }
                break;

            case 'Escape':
                this.hideNewGuestSuggestions();
                break;
        }
    }

    updateNewGuestHighlight(index) {
        const suggestions = document.querySelectorAll('#newGuestSuggestions .suggestion-item');
        suggestions.forEach((item, i) => {
            item.classList.toggle('highlighted', i === index);
        });
    }

    formatCurrentDates(dates, reservationId) {
        if (!dates || (Array.isArray(dates) && dates.length === 0) || (!Array.isArray(dates) && dates.size === 0)) {
            return '<p>Nenhuma data definida</p>';
        }
        
        try {
            const datesArray = Array.isArray(dates) ? dates : Array.from(dates);
            const stringDates = datesArray.map(date => {
                if (typeof date === 'string') {
                    return date;
                } else if (typeof date === 'object' && date !== null) {
                    if (date.year && date.month && date.day) {
                        return `${date.year}-${String(date.month).padStart(2, '0')}-${String(date.day).padStart(2, '0')}`;
                    }
                    if (date.$date) {
                        return date.$date.substring(0, 10);
                    }
                    const dateStr = String(date);
                    if (dateStr.match(/\d{4}-\d{2}-\d{2}/)) {
                        return dateStr;
                    }
                }
                return '';
            }).filter(dateStr => dateStr !== '');
            
            const sortedDates = stringDates.sort((a, b) => {
                const dateA = this.parseDate(a);
                const dateB = this.parseDate(b);
                return dateA - dateB;
            });

            return sortedDates.map(dateStr => {
                const parsedDate = this.parseDate(dateStr);
                const formattedDate = this.formatDateForDisplay(parsedDate);
                return `
                    <div class="date-item">
                        <span>${formattedDate}</span>
                        <button class="btn btn-sm btn-danger" onclick="calendarSystem.removeDate(${reservationId}, '${dateStr}')">
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

    openAddDatesModal(reservationId) {
        this.currentReservationId = reservationId;
        document.getElementById('addDatesModal').style.display = 'block';
        
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

            const isAvailable = true;
            
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

        const guestExists = this.allGuests.some(g => 
            g.name && g.name.toLowerCase() === guestName.toLowerCase()
        );
        
        if (!guestExists) {
            const createNew = await Swal.fire({
                title: 'Hóspede não encontrado',
                text: `O hóspede "${guestName}" não existe. Deseja criá-lo?`,
                icon: 'question',
                showCancelButton: true,
                confirmButtonText: 'Sim, criar hóspede',
                cancelButtonText: 'Cancelar'
            });
            
            if (!createNew.isConfirmed) {
                return;
            }
            
            this.openCreateGuestModal();
            document.getElementById('createGuestName').value = guestName;
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
                this.hideNewGuestSuggestions();
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

    openCreateGuestModal() {
        const modal = document.getElementById('createGuestModal');
        modal.style.display = 'block';
        
        document.getElementById('createGuestName').value = '';
        document.getElementById('createGuestRg').value = '';
        document.getElementById('createGuestPhone').value = '';
        document.getElementById('createGuestEmail').value = '';
    }

    closeCreateGuestModal() {
        document.getElementById('createGuestModal').style.display = 'none';
    }

    async submitCreateGuest() {
        const name = document.getElementById('createGuestName').value.trim();
        const rg = document.getElementById('createGuestRg').value.trim();
        const phone = document.getElementById('createGuestPhone').value.trim();
        const email = document.getElementById('createGuestEmail').value.trim();
        
        if (!name || !rg) {
            this.showAlert('Nome e RG são obrigatórios', 'error');
            return;
        }
        
        try {
            const guestData = {
                name: name,
                rg: rg,
                phone: phone || 'Não informado',
                email: email || ''
            };
            
            const response = await this.makeAuthenticatedRequest('/guest/insert', {
                method: 'POST',
                body: JSON.stringify(guestData)
            });
            
            if (response.ok) {
                const newGuest = await response.json();
                this.allGuests.push(newGuest);
                
                const newGuestInput = document.getElementById('newGuestName');
                if (newGuestInput) {
                    newGuestInput.value = newGuest.name;
                }
                
                this.closeCreateGuestModal();
                this.showAlert('Hóspede criado com sucesso!', 'success');
            } else {
                throw new Error('Erro ao criar hóspede');
            }
        } catch (error) {
            console.error('Erro ao criar hóspede:', error);
            this.showAlert('Erro ao criar hóspede: ' + error.message, 'error');
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

document.addEventListener('DOMContentLoaded', () => {
    window.calendarSystem = new CalendarSystem();
});

window.onclick = function(event) {
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });
    
    if (window.calendarSystem && window.calendarSystem.expandedDate) {
        const calendar = document.getElementById('calendar');
        if (!calendar.contains(event.target)) {
            window.calendarSystem.expandedDate = null;
            window.calendarSystem.hideReservationsList();
            window.calendarSystem.loadCalendar();
        }
    }
};

document.addEventListener('click', (e) => {
    const guestInput = document.getElementById('guestName');
    const guestSuggestions = document.getElementById('guestSuggestions');
    const roomInput = document.getElementById('roomNumber');
    const roomSuggestions = document.getElementById('roomSuggestions');
    
    if (!guestInput.contains(e.target) && !guestSuggestions.contains(e.target)) {
        guestSuggestions.classList.remove('show');
    }
    
    if (!roomInput.contains(e.target) && !roomSuggestions.contains(e.target)) {
        roomSuggestions.classList.remove('show');
    }
});