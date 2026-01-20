class FinancialSystem {
    constructor() {
        this.baseUrl = '/financial';
        this.token = localStorage.getItem('jwtToken');
        this.charts = {};
        
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.initialize());
        } else {
            this.initialize();
        }
    }

    initialize() {
        this.initEventListeners();
        this.checkAuthentication();
        this.initializeCharts();
    }

    checkAuthentication() {
        if (!this.token) {
            window.location.href = '/login';
            return;
        }   
        this.loadUserInfo();
        this.loadDashboardData();
        this.loadYearFilter();
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
        // Botão de logout
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.logout();
            });
        }

        // Botões de ação
        document.getElementById('processAllBtn').addEventListener('click', () => this.processFinancialRecords());
        document.getElementById('processAdvancedBtn').addEventListener('click', () => this.processAdvanced());
        document.getElementById('refreshDataBtn').addEventListener('click', () => this.loadDashboardData());
        document.getElementById('filterDailyBtn').addEventListener('click', () => this.loadDailyRecords());
        document.getElementById('filterMonthlyBtn').addEventListener('click', () => this.loadMonthlyRecords());
        document.getElementById('generateReportBtn').addEventListener('click', () => this.generateReport());

        // Tabs
        document.querySelectorAll('.tab').forEach(tab => {
            tab.addEventListener('click', (e) => {
                const tabName = e.target.dataset.tab;
                this.showTab(tabName);
            });
        });

        // Inicializar filtros de data
        const today = new Date();
        const currentMonth = today.toISOString().slice(0, 7);
        document.getElementById('dailyMonthFilter').value = currentMonth;
        document.getElementById('reportMonth').value = currentMonth;
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
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/dashboard`);
            
            if (response.ok) {
                const data = await response.json();
                this.updateDashboard(data);
            } else {
                throw new Error('Erro ao carregar dados do dashboard');
            }
        } catch (error) {
            console.error('Erro ao carregar dashboard:', error);
            this.showAlert('Erro ao carregar dados financeiros', 'error');
        }
    }

    updateDashboard(data) {
        document.getElementById('dailyRevenue').textContent = this.formatCurrency(data.dailyRevenue || 0);
        document.getElementById('monthlyRevenue').textContent = this.formatCurrency(data.monthlyRevenue || 0);
        document.getElementById('annualRevenue').textContent = this.formatCurrency(data.annualRevenue || 0);
        
        if (data.dailyReservations) {
            document.getElementById('dailyReservations').textContent = `${data.dailyReservations} reservas`;
        }
        
        if (data.monthlyReservations) {
            document.getElementById('monthlyReservations').textContent = `${data.monthlyReservations} reservas`;
        }
        
        if (data.annualReservations) {
            document.getElementById('annualReservations').textContent = `${data.annualReservations} reservas`;
        }
        
        if (data.occupancyRate) {
            document.getElementById('occupancyRate').textContent = `${data.occupancyRate}%`;
        }
        
        if (data.averageDailyRate) {
            document.getElementById('averageDailyRate').textContent = this.formatCurrency(data.averageDailyRate);
        }
        
        if (data.revPAR) {
            document.getElementById('revPAR').textContent = this.formatCurrency(data.revPAR);
        }
        
        if (data.totalNights) {
            document.getElementById('totalNights').textContent = data.totalNights;
        }
        
        this.loadDailyRecords();
        this.loadMonthlyRecords();
        this.loadAnnualRecords();
    }

    async processFinancialRecords() {
        try {
            const processBtn = document.getElementById('processAllBtn');
            const originalText = processBtn.innerHTML;
            processBtn.innerHTML = '⏳ Processando...';
            processBtn.disabled = true;

            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/process/all-reservations`, {
                method: 'POST'
            });

            if (response.ok) {
                const result = await response.text();
                this.showAlert(result, 'success');
                this.loadDashboardData();
                
                setTimeout(() => {
                    this.loadDailyRecords();
                    this.loadMonthlyRecords();
                    this.loadAnnualRecords();
                }, 1000);
            } else {
                throw new Error('Erro ao processar registros');
            }
        } catch (error) {
            console.error('Erro ao processar registros:', error);
            this.showAlert('Erro ao processar registros financeiros: ' + error.message, 'error');
        } finally {
            const processBtn = document.getElementById('processAllBtn');
            processBtn.innerHTML = '🔄 Processar Todas as Reservas';
            processBtn.disabled = false;
        }
    }

    async processAdvanced() {
        try {
            const options = await this.showProcessOptions();
            
            if (!options) return;

            let response;
            
            if (options.type === 'all') {
                response = await this.makeAuthenticatedRequest(`${this.baseUrl}/process/all-reservations`, {
                    method: 'POST'
                });
            } else if (options.type === 'date') {
                response = await this.makeAuthenticatedRequest(`${this.baseUrl}/process/force/${options.date}`, {
                    method: 'POST'
                });
            } else if (options.type === 'period') {
                response = await this.makeAuthenticatedRequest(
                    `${this.baseUrl}/reprocess?startDate=${options.startDate}&endDate=${options.endDate}`, {
                    method: 'POST'
                });
            }

            if (response && response.ok) {
                const result = await response.text();
                this.showAlert(result, 'success');
                this.loadDashboardData();
            }
        } catch (error) {
            console.error('Erro no processamento avançado:', error);
            this.showAlert('Erro no processamento: ' + error.message, 'error');
        }
    }

    showProcessOptions() {
        return new Promise((resolve) => {
            const modalHtml = `
                <div id="processModal" class="modal-backdrop">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h3 class="modal-title">🔄 Opções de Processamento</h3>
                            <button class="modal-close" id="closeProcessModal">&times;</button>
                        </div>
                        <div class="modal-body">
                            <div class="radio-group">
                                <label class="radio-option" id="radioAll">
                                    <input type="radio" name="processType" value="all" checked> 
                                    <div class="radio-label">
                                        <strong>Processar TODAS as reservas existentes</strong>
                                        <div class="radio-description">Processa todas as reservas confirmadas do sistema</div>
                                    </div>
                                </label>
                                
                                <label class="radio-option" id="radioDate">
                                    <input type="radio" name="processType" value="date"> 
                                    <div class="radio-label">
                                        <strong>Processar data específica</strong>
                                        <div class="radio-description">Processa apenas reservas de uma data específica</div>
                                    </div>
                                </label>
                                <div class="date-inputs">
                                    <input type="date" id="specificDate" class="form-control">
                                </div>
                                
                                <label class="radio-option" id="radioPeriod">
                                    <input type="radio" name="processType" value="period"> 
                                    <div class="radio-label">
                                        <strong>Processar período</strong>
                                        <div class="radio-description">Processa reservas em um intervalo de datas</div>
                                    </div>
                                </label>
                                <div class="date-inputs">
                                    <div class="date-range">
                                        <input type="date" id="startDate" class="form-control" placeholder="Data inicial">
                                        <input type="date" id="endDate" class="form-control" placeholder="Data final">
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button class="btn" id="cancelProcessBtn" style="background: #ddd;">
                                Cancelar
                            </button>
                            <button class="btn btn-primary" id="confirmProcessBtn">
                                Processar
                            </button>
                        </div>
                    </div>
                </div>
            `;
            
            document.body.insertAdjacentHTML('beforeend', modalHtml);
            
            // Inicializar datas
            const today = new Date().toISOString().split('T')[0];
            document.getElementById('specificDate').value = today;
            document.getElementById('startDate').value = today;
            document.getElementById('endDate').value = today;
            
            // Configurar eventos do modal
            this.setupProcessModalEvents(resolve);
            
            window.processModalResolve = resolve;
        });
    }

    setupProcessModalEvents(resolve) {
        // Seleção de opções de rádio
        document.querySelectorAll('.radio-option').forEach(option => {
            option.addEventListener('click', (e) => {
                e.currentTarget.querySelector('input').checked = true;
                this.updateRadioSelection(e.currentTarget);
            });
        });
        
        // Botão de fechar
        document.getElementById('closeProcessModal').addEventListener('click', () => {
            this.closeProcessModal(null);
        });
        
        // Botão cancelar
        document.getElementById('cancelProcessBtn').addEventListener('click', () => {
            this.closeProcessModal(null);
        });
        
        // Botão confirmar
        document.getElementById('confirmProcessBtn').addEventListener('click', () => {
            this.confirmProcess();
        });
        
        // Atualizar seleção inicial
        this.updateRadioSelection(document.querySelector('.radio-option'));
    }

    updateRadioSelection(selectedOption) {
        document.querySelectorAll('.radio-option').forEach(option => {
            option.classList.remove('selected');
        });                
        selectedOption.classList.add('selected');
    }

    closeProcessModal(result) {
        const modal = document.getElementById('processModal');
        if (modal) {
            modal.remove();
        }
        if (window.processModalResolve) {
            window.processModalResolve(result);
            window.processModalResolve = null;
        }
    }

    confirmProcess() {
        const selectedType = document.querySelector('input[name="processType"]:checked').value;
        let options = { type: selectedType };
        
        if (selectedType === 'date') {
            const date = document.getElementById('specificDate').value;
            if (!date) {
                alert('Por favor, selecione uma data.');
                return;
            }
            options.date = date;
        } else if (selectedType === 'period') {
            const startDate = document.getElementById('startDate').value;
            const endDate = document.getElementById('endDate').value;
            if (!startDate || !endDate) {
                alert('Por favor, selecione ambas as datas.');
                return;
            }
            if (startDate > endDate) {
                alert('A data inicial não pode ser maior que a data final.');
                return;
            }
            options.startDate = startDate;
            options.endDate = endDate;
        }
        
        this.closeProcessModal(options);
    }

    async loadDailyRecords() {
        try {
            const monthFilter = document.getElementById('dailyMonthFilter').value;
            const [year, month] = monthFilter.split('-');
            
            const startDate = new Date(year, month - 1, 1);
            const endDate = new Date(year, month, 0);

            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/daily?startDate=${startDate.toISOString().split('T')[0]}&endDate=${endDate.toISOString().split('T')[0]}`
            );

            if (response.ok) {
                const records = await response.json();
                this.displayDailyRecords(records);
                this.renderDailyChart(records);
            }
        } catch (error) {
            console.error('Erro ao carregar registros diários:', error);
            document.getElementById('dailyRecords').innerHTML = '<p>Erro ao carregar registros diários</p>';
        }
    }

    async loadMonthlyRecords() {
        try {
            const year = document.getElementById('yearFilter').value || new Date().getFullYear();
            
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/monthly?year=${year}`);

            if (response.ok) {
                const records = await response.json();
                this.displayMonthlyRecords(records);
                this.renderMonthlyChart(records);
            }
        } catch (error) {
            console.error('Erro ao carregar registros mensais:', error);
            document.getElementById('monthlyRecords').innerHTML = '<p>Erro ao carregar registros mensais</p>';
        }
    }

    async loadAnnualRecords() {
        try {
            const response = await this.makeAuthenticatedRequest(`${this.baseUrl}/annual`);

            if (response.ok) {
                const records = await response.json();
                this.displayAnnualRecords(records);
                this.renderAnnualChart(records);
            }
        } catch (error) {
            console.error('Erro ao carregar registros anuais:', error);
            document.getElementById('annualRecords').innerHTML = '<p>Erro ao carregar registros anuais</p>';
        }
    }

    displayDailyRecords(records) {
        const container = document.getElementById('dailyRecords');
        
        if (!records || records.length === 0) {
            container.innerHTML = '<p>Nenhum registro diário encontrado para o período selecionado.</p>';
            return;
        }

        const html = `
            <table class="table">
                <thead>
                    <tr>
                        <th>Data</th>
                        <th>Receita</th>
                        <th>Reservas</th>
                        <th>Hóspedes</th>
                        <th>Noites</th>
                        <th>ADR</th>
                        <th>Ocupação</th>
                    </tr>
                </thead>
                <tbody>
                    ${records.map(record => `
                        <tr>
                            <td>${this.formatDate(record.recordDate)}</td>
                            <td>${this.formatCurrency(record.totalRevenue)}</td>
                            <td>${record.totalReservations}</td>
                            <td>${record.totalGuests}</td>
                            <td>${record.totalNights}</td>
                            <td>${this.formatCurrency(record.averageDailyRate)}</td>
                            <td>${record.occupancyRate ? record.occupancyRate.toFixed(1) + '%' : 'N/A'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;

        container.innerHTML = html;
    }

    displayMonthlyRecords(records) {
        const container = document.getElementById('monthlyRecords');
        
        if (!records || records.length === 0) {
            container.innerHTML = '<p>Nenhum registro mensal encontrado.</p>';
            return;
        }

        const html = `
            <table class="table">
                <thead>
                    <tr>
                        <th>Mês</th>
                        <th>Receita</th>
                        <th>Reservas</th>
                        <th>Hóspedes</th>
                        <th>Noites</th>
                        <th>ADR</th>
                        <th>Ocupação</th>
                        <th>RevPAR</th>
                    </tr>
                </thead>
                <tbody>
                    ${records.map(record => `
                        <tr>
                            <td>${this.formatMonth(record.recordDate)}</td>
                            <td>${this.formatCurrency(record.totalRevenue)}</td>
                            <td>${record.totalReservations}</td>
                            <td>${record.totalGuests}</td>
                            <td>${record.totalNights}</td>
                            <td>${this.formatCurrency(record.averageDailyRate)}</td>
                            <td>${record.occupancyRate ? record.occupancyRate.toFixed(1) + '%' : 'N/A'}</td>
                            <td>${this.formatCurrency(record.revPAR)}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;

        container.innerHTML = html;
    }

    displayAnnualRecords(records) {
        const container = document.getElementById('annualRecords');
        
        if (!records || records.length === 0) {
            container.innerHTML = '<p>Nenhum registro anual encontrado.</p>';
            return;
        }

        const html = `
            <table class="table">
                <thead>
                    <tr>
                        <th>Ano</th>
                        <th>Receita</th>
                        <th>Reservas</th>
                        <th>Hóspedes</th>
                        <th>Noites</th>
                        <th>ADR</th>
                        <th>Ocupação</th>
                        <th>RevPAR</th>
                    </tr>
                </thead>
                <tbody>
                    ${records.map(record => `
                        <tr>
                            <td>${new Date(record.recordDate).getFullYear()}</td>
                            <td>${this.formatCurrency(record.totalRevenue)}</td>
                            <td>${record.totalReservations}</td>
                            <td>${record.totalGuests}</td>
                            <td>${record.totalNights}</td>
                            <td>${this.formatCurrency(record.averageDailyRate)}</td>
                            <td>${record.occupancyRate ? record.occupancyRate.toFixed(1) + '%' : 'N/A'}</td>
                            <td>${this.formatCurrency(record.revPAR)}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;

        container.innerHTML = html;
    }

    async generateReport() {
        try {
            const month = document.getElementById('reportMonth').value;
            const [year, monthNum] = month.split('-');
            
            const response = await this.makeAuthenticatedRequest(
                `${this.baseUrl}/monthly?year=${year}`
            );

            if (response.ok) {
                const records = await response.json();
                const monthlyRecord = records.find(r => 
                    new Date(r.recordDate).getMonth() + 1 === parseInt(monthNum)
                );
                
                this.displayReport(monthlyRecord, month);
            }
        } catch (error) {
            console.error('Erro ao gerar relatório:', error);
            this.showAlert('Erro ao gerar relatório', 'error');
        }
    }

    displayReport(record, month) {
        const container = document.getElementById('reportContent');
        
        if (!record) {
            container.innerHTML = '<p>Nenhum dado encontrado para o mês selecionado.</p>';
            return;
        }

        const monthNames = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
                          'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];
        const monthName = monthNames[new Date(month + '-01').getMonth()];

        const html = `
            <div class="report-summary">
                <h4>Relatório de ${monthName}</h4>
                <div class="metrics-grid">
                    <div class="metric-card">
                        <div class="metric-value">${this.formatCurrency(record.totalRevenue)}</div>
                        <div class="metric-label">Receita Total</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">${record.totalReservations}</div>
                        <div class="metric-label">Total de Reservas</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">${record.totalGuests}</div>
                        <div class="metric-label">Total de Hóspedes</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">${record.totalNights}</div>
                        <div class="metric-label">Total de Noites</div>
                    </div>
                </div>
                
                <h5 style="margin-top: 2rem;">Métricas de Desempenho</h5>
                <div class="metrics-grid">
                    <div class="metric-card">
                        <div class="metric-value">${this.formatCurrency(record.averageDailyRate)}</div>
                        <div class="metric-label">ADR</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">${record.occupancyRate ? record.occupancyRate.toFixed(1) + '%' : 'N/A'}</div>
                        <div class="metric-label">Taxa de Ocupação</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">${this.formatCurrency(record.revPAR)}</div>
                        <div class="metric-label">RevPAR</div>
                    </div>
                </div>
            </div>
        `;

        container.innerHTML = html;

        if (record.roomTypeRevenues && record.roomTypeRevenues.length > 0) {
            this.renderRoomTypeChart(record.roomTypeRevenues);
        }
    }

    initializeCharts() {
        this.charts.daily = this.createChart('dailyChart', 'line', 'Receita Diária');
        this.charts.monthly = this.createChart('monthlyChart', 'bar', 'Receita Mensal');
        this.charts.annual = this.createChart('annualChart', 'bar', 'Receita Anual');
        this.charts.roomType = this.createChart('roomTypeChart', 'doughnut', 'Receita por Tipo de Quarto');
    }

    createChart(canvasId, type, label) {
        const ctx = document.getElementById(canvasId).getContext('2d');
        return new Chart(ctx, {
            type: type,
            data: {
                labels: [],
                datasets: [{
                    label: label,
                    data: [],
                    backgroundColor: this.getChartColors(type),
                    borderColor: '#FF6B35',
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: type === 'line' || type === 'bar' ? {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                return 'R$ ' + value.toLocaleString('pt-BR');
                            }
                        }
                    }
                } : {}
            }
        });
    }

    renderDailyChart(records) {
        if (!records || records.length === 0) return;

        const labels = records.map(r => this.formatDate(r.recordDate));
        const data = records.map(r => parseFloat(r.totalRevenue));

        this.charts.daily.data.labels = labels;
        this.charts.daily.data.datasets[0].data = data;
        this.charts.daily.update();
    }

    renderMonthlyChart(records) {
        if (!records || records.length === 0) return;

        const labels = records.map(r => this.formatMonth(r.recordDate));
        const data = records.map(r => parseFloat(r.totalRevenue));

        this.charts.monthly.data.labels = labels;
        this.charts.monthly.data.datasets[0].data = data;
        this.charts.monthly.update();
    }

    renderAnnualChart(records) {
        if (!records || records.length === 0) return;

        const labels = records.map(r => new Date(r.recordDate).getFullYear());
        const data = records.map(r => parseFloat(r.totalRevenue));

        this.charts.annual.data.labels = labels;
        this.charts.annual.data.datasets[0].data = data;
        this.charts.annual.update();
    }

    renderRoomTypeChart(roomTypeRevenues) {
        if (!roomTypeRevenues || roomTypeRevenues.length === 0) return;

        const labels = roomTypeRevenues.map(r => this.getRoomTypeName(r.roomType));
        const data = roomTypeRevenues.map(r => parseFloat(r.revenue));

        this.charts.roomType.data.labels = labels;
        this.charts.roomType.data.datasets[0].data = data;
        this.charts.roomType.update();
    }

    showTab(tabName) {
        // Remover classe active de todas as tabs e conteúdos
        document.querySelectorAll('.tab-content').forEach(tab => {
            tab.classList.remove('active');
        });
        document.querySelectorAll('.tab').forEach(tab => {
            tab.classList.remove('active');
        });

        // Adicionar classe active à tab selecionada
        document.getElementById(`tab-${tabName}`).classList.add('active');
        document.querySelector(`.tab[data-tab="${tabName}"]`).classList.add('active');

        // Carregar dados da tab se necessário
        if (tabName === 'daily') {
            this.loadDailyRecords();
        } else if (tabName === 'monthly') {
            this.loadMonthlyRecords();
        } else if (tabName === 'annual') {
            this.loadAnnualRecords();
        }
    }

    loadYearFilter() {
        const yearSelect = document.getElementById('yearFilter');
        const currentYear = new Date().getFullYear();
        
        for (let year = currentYear; year >= currentYear - 5; year--) {
            const option = document.createElement('option');
            option.value = year;
            option.textContent = year;
            if (year === currentYear) option.selected = true;
            yearSelect.appendChild(option);
        }
    }

    formatCurrency(value) {
        return new Intl.NumberFormat('pt-BR', {
            style: 'currency',
            currency: 'BRL'
        }).format(value || 0);
    }

    formatDate(dateString) {
        return new Date(dateString).toLocaleDateString('pt-BR');
    }

    formatMonth(dateString) {
        return new Date(dateString).toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
    }

    getRoomTypeName(roomType) {
        const roomTypes = {
            'SHARED': 'Compartilhado',
            'EXCLUSIVE': 'Exclusivo',
            'SUITE': 'Suíte',
            'STUDIO': 'Studio',
            'ROOM_SHARED_BATHROOM': 'Quarto c/ Banheiro Compartilhado'
        };
        return roomTypes[roomType] || roomType;
    }

    getChartColors(type) {
        if (type === 'doughnut') {
            return ['#FF6B35', '#4A90E2', '#9B59B6', '#90EE90', '#F39C12'];
        }
        return '#FF6B35';
    }

    showAlert(message, type) {
        const alertElement = document.getElementById(`alert${type.charAt(0).toUpperCase() + type.slice(1)}`);
        alertElement.textContent = message;
        alertElement.style.display = 'block';

        setTimeout(() => {
            alertElement.style.display = 'none';
        }, 5000);
    }
}
window.financialSystem = new FinancialSystem();