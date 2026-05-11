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
        this.establishmentInfo = null;
        
        this.initEventListeners();
        this.checkAuthentication();
        this.loadGuests();
        this.loadRooms();
        this.loadCalendar();
        this.loadReservations();
        this.loadEstablishmentInfo();
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
            const userEmail = localStorage.getItem('userInfo') || localStorage.getItem('userEmail');
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
                console.log('Hóspedes carregados:', this.allGuests.length);
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

    async loadEstablishmentInfo() {
        try {
            const userEmail = localStorage.getItem('userInfo') || localStorage.getItem('userEmail');
            
            if (!userEmail) {
                console.error('Email do usuário não encontrado no localStorage');
                this.establishmentInfo = this.getDefaultEstablishmentInfo();
                return this.establishmentInfo;
            }
            
            console.log('Carregando informações para o email:', userEmail);
            
            const response = await this.makeAuthenticatedRequest('/us/establishment-info', {
                method: 'POST',
                body: JSON.stringify({ email: userEmail })
            });
            
            if (response.ok) {
                this.establishmentInfo = await response.json();
                console.log('Informações do estabelecimento carregadas:', this.establishmentInfo);
                return this.establishmentInfo;
            } else {
                console.error('Erro ao carregar informações, status:', response.status);
                this.establishmentInfo = this.getDefaultEstablishmentInfo();
                return this.establishmentInfo;
            }
        } catch (error) {
            console.error('Erro ao carregar informações do estabelecimento:', error);
            this.establishmentInfo = this.getDefaultEstablishmentInfo();
            return this.establishmentInfo;
        }
    }

    getDefaultEstablishmentInfo() {
        return {
            establishmentName: 'Elô AP',
            establishmentAddress: 'QNM 16 Conjunto B',
            establishmentPhone: '(61) 99999-9999',
            establishmentResponsible: 'Elô',
            establishmentLogo: '',
            establishmentWelcomeMessage: ''
        };
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
        
        const settingsBtn = document.getElementById('settingsBtn');
        if (settingsBtn) {
            settingsBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.openEstablishmentSettings();
            });
        }
        
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
            let isSharedRoom = false;
            
            const room = this.allRooms.find(r => r.number === roomNumber);
            if (room && (room.sharedRoom || room.sharedBathroom)) {
                isSharedRoom = true;
            }

            for (const dateStr of dateStrings) {
                const reservedRooms = this.reservedDates.get(dateStr) || [];
                if (reservedRooms.includes(roomNumber)) {
                    if (!isSharedRoom) {
                        hasConflict = true;
                        conflictDate = dateStr;
                        break;
                    }
                }
            }

            if (hasConflict) {
                return {
                    available: false,
                    message: `Quarto ${roomNumber} já está reservado para ${this.formatDateForDisplay(conflictDate)}`
                };
            }

            if (isSharedRoom) {
                return {
                    available: true,
                    message: `Quarto compartilhado ${roomNumber} - sistema verificará disponibilidade de cama`
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
        localStorage.removeItem('userEmail');
        localStorage.removeItem('userName');
        window.location.href = '/login';
    }

    async makeAuthenticatedRequest(url, options = {}) {
        this.token = localStorage.getItem('jwtToken');
        
        if (!this.token) {
            this.logout();
            throw new Error('Token de autenticação não encontrado');
        }

        const tokenExpiry = localStorage.getItem('tokenExpiry');
        if (tokenExpiry && Date.now() > parseInt(tokenExpiry)) {
            this.logout();
            throw new Error('Sessão expirada. Faça login novamente.');
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
        
        // MODIFICAÇÃO: Apenas adiciona a classe 'past' para estilização visual
        // Não impede mais o clique
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
            } else if (reservedRooms.length >= totalRooms && totalRooms > 0) {
                const fullyOccupiedElement = document.createElement('div');
                fullyOccupiedElement.className = 'event';
                fullyOccupiedElement.style.backgroundColor = '#ff6b6b';
                fullyOccupiedElement.textContent = `Todos lotados`;
                fullyOccupiedElement.title = `Todos os quartos/camas estão ocupados, mas você ainda pode gerenciar reservas`;
                eventsElement.appendChild(fullyOccupiedElement);
            }
            
            const isFormVisible = document.getElementById('reservationForm').style.display === 'block';
            // MODIFICAÇÃO: Removida a verificação de data passada para seleção
            if (isFormVisible) {
                const selectIndicator = document.createElement('div');
                selectIndicator.className = 'event available';
                selectIndicator.style.fontSize = '0.6rem';
                selectIndicator.style.marginTop = '2px';
                selectIndicator.style.background = 'var(--blue-light)';
                selectIndicator.style.color = 'white';
                selectIndicator.textContent = 'Clique para selecionar';
                eventsElement.appendChild(selectIndicator);
            }
        } else {
            this.createExpandedDateContent(dateString, eventsElement);
        }
        
        dayElement.appendChild(eventsElement);
        
        // MODIFICAÇÃO: TODAS as datas são clicáveis, inclusive as passadas
        dayElement.style.cursor = 'pointer';
        dayElement.addEventListener('click', (e) => {
            e.stopPropagation();
            this.toggleDateExpansion(date);
        });
        
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
            occupiedTitle.textContent = 'Reservas na data:';
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
                eventElement.style.cursor = 'pointer';
                eventElement.innerHTML = `
                    <strong>Reserva #${reservation.id}</strong> - Q${roomNumber}
                    <br><small>${guestName} (${status})</small>
                `;
                eventElement.title = `Clique para gerenciar - Hóspede: ${guestName} - Status: ${status}`;
                
                eventElement.addEventListener('click', (e) => {
                    e.stopPropagation();
                    this.manageReservation(reservation.id);
                });
                
                container.appendChild(eventElement);
            });
        } else {
            const noReservationsElement = document.createElement('div');
            noReservationsElement.className = 'event-title';
            noReservationsElement.textContent = 'Nenhuma reserva nesta data';
            noReservationsElement.style.color = '#666';
            noReservationsElement.style.fontSize = '0.7rem';
            container.appendChild(noReservationsElement);
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
        } else if (availableRooms === 0 && totalRooms > 0) {
            const fullyOccupiedElement = document.createElement('div');
            fullyOccupiedElement.className = 'event';
            fullyOccupiedElement.style.backgroundColor = '#ff6b6b';
            fullyOccupiedElement.style.cursor = 'pointer';
            fullyOccupiedElement.innerHTML = `<strong>⚠️ Data totalmente ocupada</strong><br><small>Todos os ${totalRooms} quartos/camas reservados</small>`;
            fullyOccupiedElement.title = `Todos os quartos estão ocupados nesta data. Você ainda pode gerenciar as reservas existentes.`;
            
            fullyOccupiedElement.addEventListener('click', (e) => {
                e.stopPropagation();
                const reservationsForDate = this.getReservationsForDate(dateString);
                if (reservationsForDate.length > 0) {
                    this.expandedDate = dateString;
                    this.showReservationsForDate(dateString);
                    this.loadCalendar();
                } else {
                    this.showAlert('Não há reservas para gerenciar nesta data', 'info');
                }
            });
            
            container.appendChild(fullyOccupiedElement);
            
            const forceReserveElement = document.createElement('div');
            forceReserveElement.className = 'event';
            forceReserveElement.style.backgroundColor = '#4a90e2';
            forceReserveElement.style.cursor = 'pointer';
            forceReserveElement.style.marginTop = '4px';
            forceReserveElement.innerHTML = `<strong>➕ Nova Reserva</strong><br><small>Para quarto compartilhado</small>`;
            forceReserveElement.title = `Forçar criação de reserva (verifica disponibilidade de cama no backend)`;
            
            forceReserveElement.addEventListener('click', (e) => {
                e.stopPropagation();
                const form = document.getElementById('reservationForm');
                if (form.style.display !== 'block') {
                    form.style.display = 'block';
                    this.selectedDates.add(dateString);
                    this.updateSelectedDatesDisplay();
                    this.loadCalendar();
                    form.scrollIntoView({ behavior: 'smooth' });
                    this.showAlert(`Data ${this.formatDateForDisplay(dateString)} selecionada. O backend verificará disponibilidade de cama.`, 'info');
                }
            });
            
            container.appendChild(forceReserveElement);
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
        
        // MODIFICAÇÃO: Não bloqueia mais a seleção de datas passadas
        // Apenas mantém a lógica de adicionar/remover datas selecionadas
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
        
        const guestName = this.getGuestName(reservation);
        
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
                        <div class="action-card" onclick="calendarSystem.openAdvancedEditModal(${reservation.id})">
                            <div class="action-icon">✏️</div>
                            <div class="action-title">Edição Avançada</div>
                            <div class="action-description">Editar valores, hóspedes e datas</div>
                        </div>
                    </div>
                </div>

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

                <div class="management-section voucher-section">
                    <h4 class="section-title">📄 Voucher de Hospedagem</h4>
                    <div class="voucher-preview-modern" id="modernVoucherPreview">
                        ${this.generateModernVoucherHTML(reservation)}
                    </div>
                    <div class="print-controls" style="margin-top: 1rem;">
                        <button class="btn btn-primary btn-voucher" onclick="calendarSystem.downloadModernVoucher(${reservation.id})">
                            <i class="fas fa-download"></i> Baixar Voucher (PDF)
                        </button>
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
        const dailyRate = reservation.dailyRate || 0;
        const numberOfDays = reservation.reservedDays ? 
            (Array.isArray(reservation.reservedDays) ? reservation.reservedDays.length : reservation.reservedDays.size) : 0;
        const extraGuestDailyFee = reservation.extraGuestDailyFee || 0;
        const numberOfExtraGuests = Math.max(0, (reservation.guest ? 
            (Array.isArray(reservation.guest) ? reservation.guest.length : reservation.guest.size) : 0) - 1);
        
        const baseTotal = dailyRate * numberOfDays;
        const extraTotal = extraGuestDailyFee * numberOfExtraGuests * numberOfDays;
        
        let finalTotal = baseTotal + extraTotal;
        
        if (reservation.useCustomAmount === true && reservation.customTotalAmount) {
            finalTotal = parseFloat(reservation.customTotalAmount);
        }
        
        return finalTotal.toFixed(2);
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

    generateModernVoucherHTML(reservation) {
        const guestName = this.getGuestName(reservation);
        const checkInDate = this.getFirstReservationDate(reservation);
        const checkOutDate = this.getLastReservationDate(reservation);
        const roomNumber = this.getRoomNumber(reservation);
        const guestCount = reservation.guest?.length || 1;
        const daysCount = reservation.reservedDays?.length || 0;
        
        const dailyRate = reservation.dailyRate || 0;
        const extraGuestDailyFee = reservation.extraGuestDailyFee || 0;
        const extraGuests = Math.max(0, guestCount - 1);
        const baseTotal = dailyRate * daysCount;
        const extraTotal = extraGuestDailyFee * extraGuests * daysCount;
        let total = baseTotal + extraTotal;
        
        if (reservation.useCustomAmount === true && reservation.customTotalAmount) {
            total = parseFloat(reservation.customTotalAmount);
        }
        
        const estInfo = this.establishmentInfo || this.getDefaultEstablishmentInfo();
        
        let welcomeMessage;
        if (estInfo.establishmentWelcomeMessage && estInfo.establishmentWelcomeMessage.trim() !== '') {
            welcomeMessage = estInfo.establishmentWelcomeMessage.replace('{guest}', guestName);
        } else {
            welcomeMessage = this.getRandomWelcomeMessage(guestName);
        }
        
        const logoHtml = estInfo.establishmentLogo ? 
            `<img src="${estInfo.establishmentLogo}" alt="Logo" style="max-height: 50px; margin-bottom: 10px;">` : '';
        
        return `
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 20px; padding: 2px; box-shadow: 0 10px 40px rgba(0,0,0,0.1);">
                <div style="background: white; border-radius: 18px; padding: 25px;">
                    <div style="text-align: center; margin-bottom: 25px;">
                        ${logoHtml}
                        <div style="background: linear-gradient(135deg, #667eea, #764ba2); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;">
                            <h1 style="font-size: 28px; margin: 0; letter-spacing: 2px;">🏨 ${this.escapeHtml(estInfo.establishmentName)}</h1>
                            <p style="font-size: 14px; margin: 5px 0 0 0; opacity: 0.7;">COMPROVANTE DE RESERVA</p>
                        </div>
                    </div>

                    <div style="background: linear-gradient(135deg, #f5f0ff 0%, #e8e0ff 100%); border-radius: 15px; padding: 20px; margin-bottom: 25px; text-align: center; border-left: 4px solid #667eea;">
                        <div style="font-size: 42px; margin-bottom: 10px;">🎉</div>
                        <h2 style="color: #4a5568; margin: 0 0 10px 0; font-size: 22px;">✨ ${this.escapeHtml(welcomeMessage)} ✨</h2>
                        <p style="color: #667eea; margin: 0; font-size: 15px;">Sua experiência começa aqui! Estamos felizes em recebê-lo(a).</p>
                    </div>

                    <div style="background: #2d3748; border-radius: 12px; padding: 15px; text-align: center; margin-bottom: 25px;">
                        <span style="color: #a0aec0; font-size: 12px; letter-spacing: 2px;">CÓDIGO DA RESERVA</span>
                        <div style="color: #ffd700; font-size: 28px; font-weight: bold; font-family: monospace;">#${reservation.id.toString().padStart(6, '0')}</div>
                    </div>

                    <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; margin-bottom: 25px;">
                        <div style="background: #f7fafc; border-radius: 12px; padding: 15px;">
                            <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 10px;">
                                <span style="font-size: 24px;">📅</span>
                                <div>
                                    <div style="font-size: 11px; color: #a0aec0;">CHECK-IN</div>
                                    <div style="font-weight: bold; color: #2d3748;">${checkInDate}</div>
                                </div>
                            </div>
                            <div style="display: flex; align-items: center; gap: 10px;">
                                <span style="font-size: 24px;">🏁</span>
                                <div>
                                    <div style="font-size: 11px; color: #a0aec0;">CHECK-OUT</div>
                                    <div style="font-weight: bold; color: #2d3748;">${checkOutDate}</div>
                                </div>
                            </div>
                        </div>
                        
                        <div style="background: #f7fafc; border-radius: 12px; padding: 15px;">
                            <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 10px;">
                                <span style="font-size: 24px;">🛏️</span>
                                <div>
                                    <div style="font-size: 11px; color: #a0aec0;">QUARTO</div>
                                    <div style="font-weight: bold; color: #2d3748;">${roomNumber}</div>
                                </div>
                            </div>
                            <div style="display: flex; align-items: center; gap: 10px;">
                                <span style="font-size: 24px;">👥</span>
                                <div>
                                    <div style="font-size: 11px; color: #a0aec0;">HÓSPEDES</div>
                                    <div style="font-weight: bold; color: #2d3748;">${guestCount} pessoa(s)</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div style="background: #ebf8ff; border-radius: 12px; padding: 15px; margin-bottom: 25px; border-left: 4px solid #4299e1;">
                        <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 10px;">
                            <span style="font-size: 28px;">👤</span>
                            <div>
                                <div style="font-size: 12px; color: #4299e1;">HÓSPEDE PRINCIPAL</div>
                                <div style="font-weight: bold; font-size: 18px; color: #2b6cb0;">${this.escapeHtml(guestName)}</div>
                            </div>
                        </div>
                        ${reservation.guest && reservation.guest.length > 1 ? `
                            <div style="margin-top: 10px; padding-top: 10px; border-top: 1px solid #bee3f8;">
                                <div style="font-size: 12px; color: #4299e1; margin-bottom: 5px;">HÓSPEDES ADICIONAIS</div>
                                ${reservation.guest.slice(1).map(g => `<span style="background: #bee3f8; padding: 4px 10px; border-radius: 20px; font-size: 13px; display: inline-block; margin: 3px;">👤 ${this.escapeHtml(g.name)}</span>`).join('')}
                            </div>
                        ` : ''}
                    </div>

                    <div style="background: #fef5e7; border-radius: 12px; padding: 15px; margin-bottom: 25px;">
                        <h3 style="color: #d69e2e; margin: 0 0 15px 0; font-size: 16px;">💰 DETALHAMENTO DE VALORES</h3>
                        <div class="value-breakdown">
                            <div class="value-breakdown-item">
                                <span>🏠 Diária:</span>
                                <span>R$ ${dailyRate.toFixed(2)} x ${daysCount} dias = R$ ${(dailyRate * daysCount).toFixed(2)}</span>
                            </div>
                            ${extraGuests > 0 ? `
                                <div class="value-breakdown-item">
                                    <span>👥 Hóspedes extras:</span>
                                    <span>R$ ${extraGuestDailyFee.toFixed(2)} x ${extraGuests} x ${daysCount} dias = R$ ${(extraGuestDailyFee * extraGuests * daysCount).toFixed(2)}</span>
                                </div>
                            ` : ''}
                            ${reservation.useCustomAmount === true && reservation.customTotalAmount ? `
                                <div class="value-breakdown-item">
                                    <span>🎯 Valor Customizado:</span>
                                    <span>R$ ${parseFloat(reservation.customTotalAmount).toFixed(2)}</span>
                                </div>
                            ` : ''}
                            <div class="value-total" style="margin-top: 12px; padding-top: 12px; border-top: 2px solid #fbd38d;">
                                <strong>VALOR TOTAL:</strong> R$ ${total.toFixed(2)}
                            </div>
                        </div>
                    </div>

                    <div style="background: #edf2f7; border-radius: 12px; padding: 15px; font-size: 12px; color: #4a5568;">
                        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 10px;">
                            <span>🏢</span>
                            <span><strong>${this.escapeHtml(estInfo.establishmentName)}</strong> - ${this.escapeHtml(estInfo.establishmentAddress)}</span>
                        </div>
                        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 10px;">
                            <span>📞</span>
                            <span>Contato: ${this.escapeHtml(estInfo.establishmentPhone)} | Responsável: ${this.escapeHtml(estInfo.establishmentResponsible)}</span>
                        </div>
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <span>💡</span>
                            <span>Apresente este voucher digital ou impresso no check-in</span>
                        </div>
                    </div>

                    <div style="text-align: center; margin-top: 20px; padding-top: 15px; border-top: 1px solid #e2e8f0;">
                        <p style="color: #a0aec0; font-size: 11px; margin: 0;">Documento gerado automaticamente pelo Sistema Interactive Edge</p>
                        <p style="color: #667eea; font-size: 12px; margin: 8px 0 0 0;">⭐ Tenha uma estadia maravilhosa! ⭐</p>
                    </div>
                </div>
            </div>
        `;
    }

    getRandomWelcomeMessage(guestName) {
        const messages = [
            `Bem-vindo(a) ao seu lar temporário, ${guestName}!`,
            `É uma alegria recebê-lo(a), ${guestName}! Aproveite cada momento!`,
            `Oi ${guestName}! Sua aventura começa agora. Curta cada segundo!`,
            `Que bom ter você aqui, ${guestName}! Desfrute de uma estadia incrível!`,
            `${guestName}, sua casa longe de casa está pronta para você!`,
            `Seja muito bem-vindo(a), ${guestName}! Estamos felizes com sua visita!`
        ];
        return messages[Math.floor(Math.random() * messages.length)];
    }

    async downloadModernVoucher(reservationId) {
        try {
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/find/${reservationId}`);
            if (response.ok) {
                const reservation = await response.json();
                
                Swal.fire({
                    title: 'Gerando Voucher...',
                    text: 'Por favor, aguarde enquanto preparamos seu voucher personalizado.',
                    allowOutsideClick: false,
                    didOpen: () => Swal.showLoading()
                });
                
                await this.generateModernPDFVoucher(reservation);
                
                Swal.close();
                this.showAlert('Voucher gerado com sucesso!', 'success');
            } else {
                throw new Error('Erro ao carregar reserva');
            }
        } catch (error) {
            console.error('Erro ao gerar voucher:', error);
            Swal.close();
            this.showAlert('Erro ao gerar voucher: ' + error.message, 'error');
        }
    }

    async generateModernPDFVoucher(reservation) {
        return new Promise(async (resolve, reject) => {
            try {
                const tempDiv = document.createElement('div');
                tempDiv.style.position = 'absolute';
                tempDiv.style.left = '-9999px';
                tempDiv.style.top = '0';
                tempDiv.style.width = '794px';
                tempDiv.style.padding = '30px';
                tempDiv.style.backgroundColor = 'white';
                tempDiv.style.fontFamily = "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif";
                tempDiv.style.boxSizing = 'border-box';
                
                tempDiv.innerHTML = this.generateModernVoucherHTML(reservation);
                document.body.appendChild(tempDiv);
                
                await new Promise(resolve => setTimeout(resolve, 500));
                
                const canvas = await html2canvas(tempDiv, {
                    scale: 2,
                    useCORS: true,
                    logging: false,
                    backgroundColor: '#ffffff'
                });
                
                document.body.removeChild(tempDiv);
                
                const { jsPDF } = window.jspdf;
                const doc = new jsPDF({
                    orientation: 'portrait',
                    unit: 'px',
                    format: 'a4'
                });
                
                const imgData = canvas.toDataURL('image/jpeg', 1.0);
                const pdfWidth = doc.internal.pageSize.getWidth();
                const pdfHeight = doc.internal.pageSize.getHeight();
                
                doc.addImage(imgData, 'JPEG', 0, 0, pdfWidth, pdfHeight);
                
                const guestName = this.getGuestName(reservation);
                const safeFileName = guestName.replace(/[^a-zA-Z0-9]/g, '_');
                const fileName = `Voucher_${reservation.id}_${safeFileName}.pdf`;
                
                doc.save(fileName);
                resolve();
                
            } catch (error) {
                console.error('Erro ao gerar PDF moderno:', error);
                reject(error);
            }
        });
    }

    async openEstablishmentSettings() {
        if (!this.establishmentInfo) {
            await this.loadEstablishmentInfo();
        }
        
        const modal = document.createElement('div');
        modal.id = 'establishmentSettingsModal';
        modal.className = 'modal';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 600px;">
                <div class="modal-header">
                    <h3 class="modal-title"><i class="fas fa-building"></i> Configurações do Estabelecimento</h3>
                    <span class="close" onclick="this.closest('.modal').remove()">&times;</span>
                </div>
                <div class="modal-body">
                    <form id="establishmentSettingsForm">
                        <div class="form-group">
                            <label for="estName" class="form-label">Nome do Estabelecimento</label>
                            <input type="text" id="estName" class="form-control" value="${this.escapeHtml(this.establishmentInfo.establishmentName || '')}">
                        </div>
                        <div class="form-group">
                            <label for="estAddress" class="form-label">Endereço</label>
                            <input type="text" id="estAddress" class="form-control" value="${this.escapeHtml(this.establishmentInfo.establishmentAddress || '')}">
                        </div>
                        <div class="form-group">
                            <label for="estPhone" class="form-label">Telefone de Contato</label>
                            <input type="text" id="estPhone" class="form-control" value="${this.escapeHtml(this.establishmentInfo.establishmentPhone || '')}">
                        </div>
                        <div class="form-group">
                            <label for="estResponsible" class="form-label">Nome do Responsável</label>
                            <input type="text" id="estResponsible" class="form-control" value="${this.escapeHtml(this.establishmentInfo.establishmentResponsible || '')}">
                        </div>
                        <div class="form-group">
                            <label for="estWelcomeMessage" class="form-label">Mensagem de Boas-vindas Personalizada (opcional)</label>
                            <textarea id="estWelcomeMessage" class="form-control" rows="3" placeholder="Deixe em branco para usar mensagens automáticas. Use {guest} para o nome do hóspede.">${this.escapeHtml(this.establishmentInfo.establishmentWelcomeMessage || '')}</textarea>
                            <small class="form-text text-muted">Se deixar em branco, serão usadas mensagens de boas-vindas aleatórias.</small>
                        </div>
                        <div class="form-group">
                            <label for="estLogo" class="form-label">URL do Logo (opcional)</label>
                            <input type="text" id="estLogo" class="form-control" value="${this.escapeHtml(this.establishmentInfo.establishmentLogo || '')}" placeholder="https://exemplo.com/logo.png">
                            <small class="form-text text-muted">Adicione uma URL de imagem para usar como logo no voucher.</small>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="this.closest('.modal').remove()">Cancelar</button>
                    <button class="btn btn-primary" onclick="calendarSystem.saveEstablishmentSettings()">
                        <i class="fas fa-save"></i> Salvar Configurações
                    </button>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        modal.style.display = 'block';
    }

    async saveEstablishmentSettings() {
        const estName = document.getElementById('estName')?.value.trim() || '';
        const estAddress = document.getElementById('estAddress')?.value.trim() || '';
        const estPhone = document.getElementById('estPhone')?.value.trim() || '';
        const estResponsible = document.getElementById('estResponsible')?.value.trim() || '';
        const estWelcomeMessage = document.getElementById('estWelcomeMessage')?.value.trim() || '';
        const estLogo = document.getElementById('estLogo')?.value.trim() || '';
        
        const userEmail = localStorage.getItem('userInfo') || localStorage.getItem('userEmail');
        
        if (!userEmail) {
            this.showAlert('Email do usuário não encontrado. Faça login novamente.', 'error');
            return;
        }
        
        try {
            const response = await this.makeAuthenticatedRequest('/us/update-establishment-info', {
                method: 'PUT',
                body: JSON.stringify({
                    email: userEmail,
                    establishmentName: estName,
                    establishmentAddress: estAddress,
                    establishmentPhone: estPhone,
                    establishmentResponsible: estResponsible,
                    establishmentWelcomeMessage: estWelcomeMessage,
                    establishmentLogo: estLogo
                })
            });
            
            if (response.ok) {
                this.showAlert('Configurações salvas com sucesso!', 'success');
                document.getElementById('establishmentSettingsModal')?.remove();
                await this.loadEstablishmentInfo();
                
                if (this.currentReservationId) {
                    await this.manageReservation(this.currentReservationId);
                }
            } else {
                const error = await response.json();
                throw new Error(error.message || 'Erro ao salvar configurações');
            }
        } catch (error) {
            console.error('Erro ao salvar configurações:', error);
            this.showAlert('Erro ao salvar configurações: ' + error.message, 'error');
        }
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
                <div class="suggestion-item no-results" style="color: #666; cursor: default;">
                    <i class="fas fa-info-circle"></i> Nenhum hóspede encontrado
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
        
        suggestionsContainer.querySelectorAll('.suggestion-item:not(.no-results)').forEach(item => {
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
        suggestionsContainer.innerHTML = '';
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
                this.showAlert('Hóspede criado com sucesso!', 'success');
                this.closeCreateGuestModal();
                
                if (this.currentReservationId) {
                    await this.addGuestToReservation(this.currentReservationId);
                }
            } else {
                throw new Error('Erro ao criar hóspede');
            }
        } catch (error) {
            console.error('Erro ao criar hóspede:', error);
            this.showAlert('Erro ao criar hóspede: ' + error.message, 'error');
        }
    }

    async openAdvancedEditModal(reservationId) {
        try {
            console.log('Abrindo edição avançada para reserva:', reservationId);
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/find/${reservationId}`);
            if (response.ok) {
                const reservation = await response.json();
                this.currentReservationId = reservationId;
                
                let existingCustomTotal = '';
                let existingDailyRate = reservation.dailyRate || 0;
                let existingExtraFee = reservation.extraGuestDailyFee || 0;
                let isUsingCustomValue = reservation.useCustomAmount === true;

                let manualAdjustmentValue = 0;
                if (isUsingCustomValue && reservation.customTotalAmount) {
                    const calculatedTotal = this.calculateReservationTotal(reservation);
                    manualAdjustmentValue = parseFloat(reservation.customTotalAmount) - parseFloat(calculatedTotal);
                }
                
                const modal = document.createElement('div');
                modal.id = 'advancedEditModal';
                modal.className = 'modal';
                modal.innerHTML = `
                    <div class="modal-content" style="max-width: 900px;">
                        <div class="modal-header">
                            <h3 class="modal-title">✏️ Edição Avançada da Reserva #${reservation.id}</h3>
                            <span class="close" onclick="this.closest('.modal').remove()">&times;</span>
                        </div>
                        <div class="modal-body">
                            <div class="management-section">
                                <h4 class="section-title">💰 Valores da Reserva</h4>
                                <div class="form-row">
                                    <div class="form-group">
                                        <label class="form-label">Valor da Diária Base (R$)</label>
                                        <input type="number" id="editDailyRate" class="form-control" step="0.01" value="${existingDailyRate}">
                                    </div>
                                    <div class="form-group">
                                        <label class="form-label">Taxa por Hóspede Extra (R$/dia)</label>
                                        <input type="number" id="editExtraGuestFee" class="form-control" step="0.01" value="${existingExtraFee}">
                                    </div>
                                </div>
                                <div class="form-group">
                                    <label class="form-label">Ajuste Manual (adicional/desconto)</label>
                                    <input type="number" id="editManualAdjustment" class="form-control" step="0.01" placeholder="Ex: -50 (desconto) ou +100 (adicional)" value="${manualAdjustmentValue}">
                                    <small class="form-text text-muted">Deixe em 0 para usar o cálculo automático. Qualquer valor aqui sobrescreverá o total final.</small>
                                </div>
                                <div class="value-breakdown" id="editValuePreview">
                                    ${this.generateValuePreviewHTML(reservation)}
                                </div>
                            </div>

                            <div class="management-section">
                                <h4 class="section-title">👥 Gerenciar Hóspedes</h4>
                                <div class="guest-list" id="editGuestList">
                                    ${this.formatGuestListWithActions(reservation.guest)}
                                </div>
                                <div class="form-group" style="margin-top: 1rem;">
                                    <label class="form-label">Adicionar Novo Hóspede</label>
                                    <div class="autocomplete-container" style="position: relative;">
                                        <input 
                                            type="text" 
                                            id="editNewGuestName" 
                                            class="form-control" 
                                            placeholder="Digite o nome do hóspede (mínimo 2 caracteres)" 
                                            autocomplete="off"
                                        >
                                        <div id="editGuestSuggestions" class="autocomplete-suggestions" style="position: absolute; z-index: 10000;"></div>
                                    </div>
                                    <div class="button-group" style="margin-top: 0.5rem; display: flex; gap: 0.5rem;">
                                        <button class="btn btn-success btn-sm" onclick="calendarSystem.addGuestToReservation(${reservation.id})">
                                            <i class="fas fa-plus"></i> Adicionar Hóspede
                                        </button>
                                        <button class="btn btn-primary btn-sm" onclick="calendarSystem.openCreateGuestModal()">
                                            <i class="fas fa-user-plus"></i> Criar Novo Hóspede
                                        </button>
                                    </div>
                                </div>
                            </div>

                            <div class="management-section">
                                <h4 class="section-title">📅 Gerenciar Datas</h4>
                                <div class="current-dates" id="editDatesList">
                                    ${this.formatDatesList(reservation.reservedDays)}
                                </div>
                                <div class="form-group" style="margin-top: 1rem;">
                                    <label class="form-label">Adicionar Data</label>
                                    <div style="display: flex; gap: 0.5rem;">
                                        <input type="date" id="editNewDate" class="form-control" style="flex: 1;">
                                        <button class="btn btn-info" onclick="calendarSystem.addDateToReservation(${reservation.id})">
                                            <i class="fas fa-calendar-plus"></i> Adicionar
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button class="btn btn-success" onclick="calendarSystem.saveReservationValues(${reservation.id})">
                                <i class="fas fa-save"></i> Salvar Alterações
                            </button>
                            <button class="btn btn-danger" onclick="this.closest('.modal').remove()">Cancelar</button>
                        </div>
                    </div>
                `;
                
                document.body.appendChild(modal);
                modal.style.display = 'block';
                
                setTimeout(() => {
                    this.initAdvancedEditAutocomplete();
                }, 100);
                
                const dailyRate = document.getElementById('editDailyRate');
                const extraFee = document.getElementById('editExtraGuestFee');
                const adjustment = document.getElementById('editManualAdjustment');
                
                const updatePreview = () => {
                    const newDaily = parseFloat(dailyRate?.value) || 0;
                    const newFee = parseFloat(extraFee?.value) || 0;
                    const newAdj = parseFloat(adjustment?.value) || 0;
                    const guestCount = reservation.guest?.length || 1;
                    const daysCount = reservation.reservedDays?.length || 0;
                    
                    const extraGuests = Math.max(0, guestCount - 1);
                    const baseTotal = newDaily * daysCount;
                    const extraTotal = newFee * extraGuests * daysCount;
                    const total = baseTotal + extraTotal + newAdj;
                    
                    const previewDiv = document.getElementById('editValuePreview');
                    if (previewDiv) {
                        previewDiv.innerHTML = `
                            <div class="value-breakdown">
                                <div class="value-breakdown-item">
                                    <span>Diária base:</span>
                                    <span>R$ ${newDaily.toFixed(2)} x ${daysCount} dias = R$ ${(newDaily * daysCount).toFixed(2)}</span>
                                </div>
                                ${extraGuests > 0 ? `
                                    <div class="value-breakdown-item">
                                        <span>Taxa hóspedes extras:</span>
                                        <span>R$ ${newFee.toFixed(2)} x ${extraGuests} x ${daysCount} dias = R$ ${(newFee * extraGuests * daysCount).toFixed(2)}</span>
                                    </div>
                                ` : ''}
                                ${newAdj !== 0 ? `
                                    <div class="value-breakdown-item">
                                        <span>Ajuste manual:</span>
                                        <span>R$ ${newAdj > 0 ? '+' : ''}${newAdj.toFixed(2)}</span>
                                    </div>
                                ` : ''}
                                <div class="value-total" style="margin-top: 12px; padding-top: 12px; border-top: 2px solid #fbd38d;">
                                    <strong>VALOR TOTAL:</strong> R$ ${total.toFixed(2)}
                                </div>
                            </div>
                        `;
                    }
                };
                
                if (dailyRate) dailyRate.addEventListener('input', updatePreview);
                if (extraFee) extraFee.addEventListener('input', updatePreview);
                if (adjustment) adjustment.addEventListener('input', updatePreview);
                
            } else {
                throw new Error('Erro ao carregar reserva');
            }
        } catch (error) {
            console.error('Erro ao abrir edição avançada:', error);
            this.showAlert('Erro ao carregar dados da reserva', 'error');
        }
    }

    initAdvancedEditAutocomplete() {
        const guestInput = document.getElementById('editNewGuestName');
        const suggestionsContainer = document.getElementById('editGuestSuggestions');
        
        if (!guestInput || !suggestionsContainer) {
            console.log('Elementos de autocomplete não encontrados');
            return;
        }
        
        console.log('Inicializando autocomplete para edição avançada');
        
        const newGuestInput = guestInput.cloneNode(true);
        guestInput.parentNode.replaceChild(newGuestInput, guestInput);
        
        newGuestInput.addEventListener('input', (e) => {
            const query = e.target.value;
            console.log('Buscando hóspede:', query);
            this.searchEditGuests(query);
        });
        
        newGuestInput.addEventListener('blur', () => {
            setTimeout(() => {
                this.hideEditGuestSuggestions();
            }, 200);
        });
        
        newGuestInput.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                this.hideEditGuestSuggestions();
                newGuestInput.blur();
            }
        });
        
        newGuestInput.id = 'editNewGuestName';
    }

    searchEditGuests(query) {
        const suggestionsContainer = document.getElementById('editGuestSuggestions');
        
        if (!query || query.length < 2) {
            if (suggestionsContainer) {
                suggestionsContainer.classList.remove('show');
                suggestionsContainer.innerHTML = '';
            }
            return;
        }
        
        const filteredGuests = this.allGuests.filter(guest => 
            guest.name && guest.name.toLowerCase().includes(query.toLowerCase())
        ).slice(0, 8);
        
        if (!suggestionsContainer) return;
        
        if (filteredGuests.length === 0) {
            suggestionsContainer.innerHTML = `
                <div class="suggestion-item no-results" style="color: #666; cursor: default;">
                    <i class="fas fa-info-circle"></i> Nenhum hóspede encontrado
                </div>
            `;
            suggestionsContainer.classList.add('show');
        } else {
            const suggestionsHTML = filteredGuests.map((guest, index) => `
                <div class="suggestion-item ${index === 0 ? 'highlighted' : ''}" 
                     data-guest-name="${this.escapeHtml(guest.name)}">
                    <div><strong>${this.escapeHtml(guest.name)}</strong></div>
                    <div class="suggestion-details">RG: ${guest.rg || 'N/A'} | Tel: ${guest.phone || 'N/A'}</div>
                </div>
            `).join('');
            suggestionsContainer.innerHTML = suggestionsHTML;
            suggestionsContainer.classList.add('show');
            
            suggestionsContainer.querySelectorAll('.suggestion-item:not(.no-results)').forEach(item => {
                item.addEventListener('click', (e) => {
                    e.stopPropagation();
                    const guestName = item.getAttribute('data-guest-name');
                    const input = document.getElementById('editNewGuestName');
                    if (input) {
                        input.value = guestName;
                        suggestionsContainer.classList.remove('show');
                        suggestionsContainer.innerHTML = '';
                    }
                });
            });
        }
    }

    hideEditGuestSuggestions() {
        const suggestionsContainer = document.getElementById('editGuestSuggestions');
        if (suggestionsContainer) {
            suggestionsContainer.classList.remove('show');
            suggestionsContainer.innerHTML = '';
        }
    }

    async addGuestToReservation(reservationId) {
        console.log('addGuestToReservation chamado para reserva:', reservationId);
        
        const guestInput = document.getElementById('editNewGuestName');
        if (!guestInput) {
            console.error('Campo editNewGuestName não encontrado');
            this.showAlert('Erro: Campo de nome não encontrado', 'error');
            return;
        }
        
        const guestName = guestInput.value.trim();
        console.log('Nome do hóspede:', guestName);
        
        if (!guestName) {
            this.showAlert('Digite o nome do hóspede', 'error');
            return;
        }
        
        const guestExists = this.allGuests.some(g => 
            g.name && g.name.toLowerCase() === guestName.toLowerCase()
        );
        
        console.log('Hóspede existe?', guestExists);
        
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
            const createGuestNameInput = document.getElementById('createGuestName');
            if (createGuestNameInput) {
                createGuestNameInput.value = guestName;
            }
            return;
        }
        
        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/add-guest/${reservationId}?nameGuest=${encodeURIComponent(guestName)}`,
                { method: 'PUT' }
            );
            
            console.log('Resposta do backend:', response.status);
            
            if (response.ok) {
                this.showAlert('Hóspede adicionado com sucesso!', 'success');
                
                if (guestInput) guestInput.value = '';
                
                const modal = document.getElementById('advancedEditModal');
                if (modal) modal.remove();
                
                await this.manageReservation(reservationId);
            } else {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Erro ao adicionar hóspede');
            }
        } catch (error) {
            console.error('Erro ao adicionar hóspede:', error);
            this.showAlert('Erro ao adicionar hóspede: ' + error.message, 'error');
        }
    }

    generateValuePreviewHTML(reservation) {
        const guestCount = reservation.guest?.length || 1;
        const daysCount = reservation.reservedDays?.length || 0;
        const extraGuests = Math.max(0, guestCount - 1);
        const dailyRate = reservation.dailyRate || 0;
        const extraGuestDailyFee = reservation.extraGuestDailyFee || 0;
        const baseTotal = dailyRate * daysCount;
        const extraTotal = extraGuestDailyFee * extraGuests * daysCount;
        let total = baseTotal + extraTotal;
        
        if (reservation.useCustomAmount === true && reservation.customTotalAmount) {
            total = parseFloat(reservation.customTotalAmount);
        }
        
        return `
            <div class="value-breakdown">
                <div class="value-breakdown-item">
                    <span>Diária base:</span>
                    <span>R$ ${dailyRate.toFixed(2)} x ${daysCount} dias = R$ ${(dailyRate * daysCount).toFixed(2)}</span>
                </div>
                ${extraGuests > 0 ? `
                    <div class="value-breakdown-item">
                        <span>Taxa hóspedes extras:</span>
                        <span>R$ ${extraGuestDailyFee.toFixed(2)} x ${extraGuests} x ${daysCount} dias = R$ ${(extraGuestDailyFee * extraGuests * daysCount).toFixed(2)}</span>
                    </div>
                ` : ''}
                ${reservation.useCustomAmount === true && reservation.customTotalAmount ? `
                    <div class="value-breakdown-item">
                        <span>Valor customizado:</span>
                        <span>R$ ${parseFloat(reservation.customTotalAmount).toFixed(2)}</span>
                    </div>
                ` : ''}
                <div class="value-total" style="margin-top: 12px; padding-top: 12px; border-top: 2px solid #fbd38d;">
                    <strong>VALOR TOTAL:</strong> R$ ${total.toFixed(2)}
                </div>
            </div>
        `;
    }

    formatGuestListWithActions(guests) {
        if (!guests || guests.length === 0) return '<p>Nenhum hóspede</p>';
        
        const guestArray = Array.isArray(guests) ? guests : Array.from(guests);
        
        return guestArray.map((guest, index) => `
            <div class="guest-item">
                <span><strong>${index === 0 ? '👤 Principal:' : '👥 Extra:'}</strong> ${this.escapeHtml(guest.name)}</span>
                ${index > 0 ? `
                    <button class="btn btn-sm btn-danger" onclick="calendarSystem.removeGuestFromReservation(${this.currentReservationId}, '${this.escapeHtml(guest.name)}')">
                        Remover
                    </button>
                ` : '<span class="badge badge-primary">Hóspede Principal</span>'}
            </div>
        `).join('');
    }

    formatDatesList(dates) {
        if (!dates || dates.length === 0) return '<p>Nenhuma data definida</p>';
        
        const datesArray = Array.isArray(dates) ? dates : Array.from(dates);
        const sortedDates = datesArray.sort();
        
        return sortedDates.map(dateStr => {
            const formattedDate = this.formatDateForDisplay(this.parseDate(dateStr));
            return `
                <div class="date-item">
                    <span>📅 ${formattedDate}</span>
                    <button class="btn btn-sm btn-danger" onclick="calendarSystem.removeDateFromReservation(${this.currentReservationId}, '${dateStr}')">
                        ×
                    </button>
                </div>
            `;
        }).join('');
    }

    async saveReservationValues(reservationId) {
        const dailyRate = parseFloat(document.getElementById('editDailyRate')?.value) || 0;
        const extraGuestDailyFee = parseFloat(document.getElementById('editExtraGuestFee')?.value) || 0;
        const manualAdjustment = parseFloat(document.getElementById('editManualAdjustment')?.value) || 0;
        
        const saveButton = document.querySelector('#advancedEditModal .btn-success');
        if (saveButton) {
            saveButton.disabled = true;
            saveButton.innerHTML = '<div class="loading loading-small"></div> Salvando...';
        }
        
        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/update-values-with-adjustment/${reservationId}`,
                {
                    method: 'PUT',
                    body: JSON.stringify({
                        dailyRate: dailyRate,
                        extraGuestDailyFee: extraGuestDailyFee,
                        manualAdjustment: manualAdjustment
                    })
                }
            );
            
            if (response.ok) {
                const updatedReservation = await response.json();
                this.showAlert('Valores atualizados com sucesso!', 'success');
                
                const modal = document.getElementById('advancedEditModal');
                if (modal) modal.remove();
                
                await this.loadReservations();
                await this.manageReservation(reservationId);
                
            } else {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Erro ao atualizar valores');
            }
        } catch (error) {
            console.error('Erro ao salvar valores:', error);
            this.showAlert('Erro ao salvar alterações: ' + error.message, 'error');
        } finally {
            if (saveButton) {
                saveButton.disabled = false;
                saveButton.innerHTML = '<i class="fas fa-save"></i> Salvar Alterações';
            }
        }
    }

    async removeGuestFromReservation(reservationId, guestName) {
        if (!confirm(`Remover "${guestName}" da reserva?`)) return;
        
        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/remove-guest/${reservationId}?nameGuest=${encodeURIComponent(guestName)}`,
                { method: 'PUT' }
            );
            
            if (response.ok) {
                this.showAlert('Hóspede removido com sucesso!', 'success');
                const modal = document.getElementById('advancedEditModal');
                if (modal) modal.remove();
                await this.openAdvancedEditModal(reservationId);
            } else {
                throw new Error('Erro ao remover hóspede');
            }
        } catch (error) {
            console.error('Erro ao remover hóspede:', error);
            this.showAlert('Erro ao remover hóspede', 'error');
        }
    }

    async addDateToReservation(reservationId) {
        const newDate = document.getElementById('editNewDate')?.value;
        if (!newDate) {
            this.showAlert('Selecione uma data', 'error');
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
                const modal = document.getElementById('advancedEditModal');
                if (modal) modal.remove();
                await this.openAdvancedEditModal(reservationId);
            } else {
                throw new Error('Erro ao adicionar data');
            }
        } catch (error) {
            console.error('Erro ao adicionar data:', error);
            this.showAlert('Erro ao adicionar data', 'error');
        }
    }

    async removeDateFromReservation(reservationId, dateToRemove) {
        if (!confirm(`Remover a data ${dateToRemove}?`)) return;
        
        try {
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/remove-date/${reservationId}?date=${dateToRemove}`,
                { method: 'PUT' }
            );
            
            if (response.ok) {
                this.showAlert('Data removida com sucesso!', 'success');
                const modal = document.getElementById('advancedEditModal');
                if (modal) modal.remove();
                await this.openAdvancedEditModal(reservationId);
            } else {
                throw new Error('Erro ao remover data');
            }
        } catch (error) {
            console.error('Erro ao remover data:', error);
            this.showAlert('Erro ao remover data', 'error');
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
        const alertError = document.getElementById('alertError');
        const alertSuccess = document.getElementById('alertSuccess');
        
        if (alertError) alertError.style.display = 'none';
        if (alertSuccess) alertSuccess.style.display = 'none';
        
        const alertElement = document.getElementById(`alert${type.charAt(0).toUpperCase() + type.slice(1)}`);
        if (alertElement) {
            alertElement.textContent = message;
            alertElement.style.display = 'block';
            
            if (type === 'success') {
                setTimeout(() => {
                    alertElement.style.display = 'none';
                }, 5000);
            }
        } else {
            console.log(`${type.toUpperCase()}: ${message}`);
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
        if (calendar && !calendar.contains(event.target)) {
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
    
    if (guestInput && !guestInput.contains(e.target) && guestSuggestions && !guestSuggestions.contains(e.target)) {
        guestSuggestions.classList.remove('show');
    }
    
    if (roomInput && !roomInput.contains(e.target) && roomSuggestions && !roomSuggestions.contains(e.target)) {
        roomSuggestions.classList.remove('show');
    }
});