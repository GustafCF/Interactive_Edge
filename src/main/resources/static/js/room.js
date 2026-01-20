// const API_BASE_URL = 'http://191.217.217.80:8080/room';
const API_BASE_URL = 'http://localhost:8080/room';
let currentRooms = [];
let currentFilter = 'all';
let currentBedRoomId = null;
let sortOrder = 'asc';

const roomsTableBody = document.getElementById('roomsTableBody');
const roomModal = document.getElementById('roomModal');
const bedModal = document.getElementById('bedModal');
const confirmModal = document.getElementById('confirmModal');
const roomForm = document.getElementById('roomForm');
const alertContainer = document.getElementById('alertContainer');
const tableLoading = document.getElementById('tableLoading');
const bedsList = document.getElementById('bedsList');

const totalRoomsElement = document.getElementById('totalRooms');
const availableRoomsElement = document.getElementById('availableRooms');
const occupiedRoomsElement = document.getElementById('occupiedRooms');
const sharedRoomsElement = document.getElementById('sharedRooms');

// Função para ordenar quartos
function sortRooms() {
    currentRooms.sort((a, b) => {
        if (sortOrder === 'asc') {
            return a.number - b.number;
        } else {
            return b.number - a.number;
        }
    });
    renderRooms();
}

// Função para alternar a ordem de classificação
function toggleSortOrder() {
    sortOrder = sortOrder === 'asc' ? 'desc' : 'asc';
    sortRooms();
    updateSortButtonText();
}

// Função para atualizar o texto do botão de ordenação
function updateSortButtonText() {
    const sortBtn = document.getElementById('sortRoomsBtn');
    if (sortBtn) {
        sortBtn.textContent = sortOrder === 'asc' ? '🔽 Ordenar' : '🔼 Ordenar';
    }
}

// Função para obter o badge do tipo de quarto
function getRoomTypeBadge(roomType) {
    switch(roomType) {
        case 'SHARED':
            return '<span class="badge badge-shared">Compartilhado</span>';
        case 'EXCLUSIVE':
            return '<span class="badge badge-exclusive">Exclusivo</span>';
        case 'SUITE':
            return '<span class="badge badge-suite">Suíte</span>';
        case 'STUDIO':
            return '<span class="badge badge-studio">Studio</span>';
        case 'ROOM_SHARED_BATHROOM':
            return '<span class="badge badge-room-shared-bathroom">Banheiro Compartilhado</span>';
        default:
            return '<span class="badge">Desconhecido</span>';
    }
}

// Função para obter o token JWT
function getToken() {
    return localStorage.getItem('jwtToken');
}

// Função para verificar autenticação
function checkAuth() {
    const token = getToken();
    if (!token) {
        window.location.href = '/login';
        return false;
    }
    
    // Verificar se o token expirou
    const tokenExpiry = localStorage.getItem('tokenExpiry');
    if (tokenExpiry && Date.now() > parseInt(tokenExpiry)) {
        logout();
        return false;
    }
    
    return true;
}

function logout() {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('tokenExpiry');
    localStorage.removeItem('userInfo');
    window.location.href = '/login';
}

async function loadUserInfo() {
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
        const userInfoElement = document.getElementById('userInfo');
        if (userInfoElement) {
            userInfoElement.textContent = 'Usuário: Erro';
        }
    }
}

// Função para mostrar alertas
function showAlert(message, type = 'success') {
    const alert = document.createElement('div');
    alert.className = `alert alert-${type}`;
    alert.textContent = message;
    
    alertContainer.appendChild(alert);
    
    setTimeout(() => {
        alert.remove();
    }, 5000);
}

// Função para fazer requisições à API
async function apiRequest(url, options = {}) {
    const token = getToken();
    
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
            showAlert('Sessão expirada. Faça login novamente.', 'error');
            logout();
            return null;
        }
        
        if (response.status === 400) {
            const errorText = await response.text();
            console.error('Bad Request details:', errorText);
            showAlert('Erro na requisição. Verifique os dados.', 'error');
            return null;
        }
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        // Para respostas sem conteúdo (como DELETE)
        if (response.status === 204) {
            return { success: true };
        }
        
        return await response.json();
    } catch (error) {
        console.error('API request failed:', error);
        showAlert('Erro na comunicação com o servidor', 'error');
        return null;
    }
}

// Carregar lista de quartos
async function loadRooms() {
    if (!checkAuth()) return;

    tableLoading.style.display = 'inline-block';
    
    const rooms = await apiRequest(`${API_BASE_URL}/all`);
    
    tableLoading.style.display = 'none';
    
    if (rooms) {
        currentRooms = rooms;
        sortRooms(); // Ordenar após carregar
        updateStatistics();
        renderRooms();
    }
}

// Atualizar estatísticas
function updateStatistics() {
    const total = currentRooms.length;
    const available = currentRooms.filter(room => room.roomStatus === 'VAGUE').length;
    const occupied = currentRooms.filter(room => room.roomStatus === 'OCCUPIED').length;
    const shared = currentRooms.filter(room => room.roomType === 'SHARED').length;

    totalRoomsElement.textContent = total;
    availableRoomsElement.textContent = available;
    occupiedRoomsElement.textContent = occupied;
    sharedRoomsElement.textContent = shared;
}

// Renderizar tabela de quartos
function renderRooms() {
    roomsTableBody.innerHTML = '';

    let filteredRooms = currentRooms;

    if (currentFilter === 'available') {
        filteredRooms = currentRooms.filter(room => room.roomStatus === 'VAGUE');
    } else if (currentFilter === 'occupied') {
        filteredRooms = currentRooms.filter(room => room.roomStatus === 'OCCUPIED');
    }

    if (filteredRooms.length === 0) {
        roomsTableBody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 2rem;">
                    Nenhum quarto encontrado
                </td>
            </tr>
        `;
        return;
    }

    filteredRooms.forEach(room => {
        const row = document.createElement('tr');
        
        const statusBadge = room.roomStatus === 'VAGUE' ? 
            '<span class="badge badge-vague">Disponível</span>' : 
            '<span class="badge badge-busy">Ocupado</span>';
        
        const typeBadge = getRoomTypeBadge(room.roomType);
        
        const bedsCount = room.beds ? room.beds.length : 0;
        
        row.innerHTML = `
            <td>${room.number}</td>
            <td>${statusBadge}</td>
            <td>${typeBadge}</td>
            <td class="price">R$ ${room.price ? room.price.toFixed(2) : '0.00'}</td>
            <td>
                ${bedsCount} camas
                <button class="btn btn-info btn-xs manage-beds-btn" data-room-id="${room.id}" data-room-number="${room.number}" title="Gerenciar Camas">
                    <i class="fas fa-bed"></i>
                </button>
            </td>
            <td>
                <button class="btn btn-info btn-sm view-calendar-btn" data-room-id="${room.id}" data-room-number="${room.number}" title="Ver Calendário">
                    <i class="fas fa-calendar"></i> Calendário
                </button>
                <button class="btn btn-primary btn-sm edit-room-btn" data-room-id="${room.id}">
                    Editar
                </button>
                <button class="btn btn-danger btn-sm delete-room-btn" data-room-id="${room.id}" data-room-number="${room.number}">
                    Excluir
                </button>
            </td>
        `;
        
        roomsTableBody.appendChild(row);
    });
    
    attachDynamicEventListeners();
}

function viewCalendar(roomId, roomNumber) {
    localStorage.setItem('selectedRoom', JSON.stringify({
        id: roomId,
        number: roomNumber
    }));
    
    window.location.href = '/room-calendar';
}

function openAddModal() {
    document.getElementById('modalTitle').textContent = 'Adicionar Quarto';
    document.getElementById('roomForm').reset();
    document.getElementById('roomId').value = '';
    roomModal.style.display = 'block';
}

async function editRoom(roomId) {
    const room = await apiRequest(`${API_BASE_URL}/find/${roomId}`);
    
    if (room) {
        document.getElementById('modalTitle').textContent = 'Editar Quarto';
        document.getElementById('roomId').value = room.id;
        document.getElementById('roomNumber').value = room.number;
        document.getElementById('roomType').value = room.roomType;
        document.getElementById('roomPrice').value = room.price;
        document.getElementById('roomStatus').value = room.roomStatus;
        
        roomModal.style.display = 'block';
    }
}

async function manageBeds(roomId, roomNumber) {
    currentBedRoomId = roomId;
    document.getElementById('bedRoomNumber').textContent = roomNumber;
    
    const room = await apiRequest(`${API_BASE_URL}/find/${roomId}`);
    if (room) {
        renderBeds(room.beds);
        bedModal.style.display = 'block';
    }
}

function renderBeds(beds) {
    bedsList.innerHTML = '';
    
    if (!beds || beds.length === 0) {
        bedsList.innerHTML = '<p>Nenhuma cama cadastrada neste quarto.</p>';
        return;
    }
    
    beds.forEach((bed, index) => {
        const bedElement = document.createElement('div');
        bedElement.className = 'bed-item';
        
        const bedStatus = bed.bedStatus === 'VAGUE' ? 
            '<span class="badge badge-bed-vague">Disponível</span>' : 
            '<span class="badge badge-bed-busy">Ocupada</span>';
        
        bedElement.innerHTML = `
            Cama ${index + 1} ${bedStatus}
        `;
        
        bedsList.appendChild(bedElement);
    });
}

// Adicionar cama
async function addBed() {
    if (!currentBedRoomId) return;
    
    const result = await apiRequest(`${API_BASE_URL}/insert-bed/${currentBedRoomId}`, {
        method: 'POST'
    });
    
    if (result) {
        showAlert('Cama adicionada com sucesso!');
        // Atualizar a lista de camas
        manageBeds(currentBedRoomId, document.getElementById('bedRoomNumber').textContent);
        // Atualizar a lista principal de quartos
        loadRooms();
    }
}

// Remover cama
async function removeBed() {
    if (!currentBedRoomId) return;
    
    const result = await apiRequest(`${API_BASE_URL}/remove-bed/${currentBedRoomId}`, {
        method: 'POST'
    });
    
    if (result) {
        showAlert('Cama removida com sucesso!');
        // Atualizar a lista de camas
        manageBeds(currentBedRoomId, document.getElementById('bedRoomNumber').textContent);
        // Atualizar a lista principal de quartos
        loadRooms();
    }
}

// Confirmar exclusão
function confirmDelete(roomId, roomNumber) {
    document.getElementById('confirmRoomNumber').textContent = roomNumber;
    document.getElementById('confirmDeleteBtn').onclick = () => deleteRoom(roomId);
    confirmModal.style.display = 'block';
}

// Excluir quarto
async function deleteRoom(roomId) {
    if (!checkAuth()) return;

    const result = await apiRequest(`${API_BASE_URL}/delete/${roomId}`, {
        method: 'DELETE'
    });

    if (result) {
        showAlert('Quarto excluído com sucesso!');
        closeModal(confirmModal);
        loadRooms();
    }
}

// Fechar modal
function closeModal(modal) {
    modal.style.display = 'none';
}

// Filtrar quartos
function filterRooms(filter) {
    currentFilter = filter;
    renderRooms();
}

// Anexar event listeners aos botões dinâmicos
function attachDynamicEventListeners() {
    // Botões de gerenciar camas
    document.querySelectorAll('.manage-beds-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const roomId = btn.getAttribute('data-room-id');
            const roomNumber = btn.getAttribute('data-room-number');
            manageBeds(roomId, roomNumber);
        });
    });
    
    // Botões de ver calendário
    document.querySelectorAll('.view-calendar-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const roomId = btn.getAttribute('data-room-id');
            const roomNumber = btn.getAttribute('data-room-number');
            viewCalendar(roomId, roomNumber);
        });
    });
    
    // Botões de editar quarto
    document.querySelectorAll('.edit-room-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const roomId = btn.getAttribute('data-room-id');
            editRoom(roomId);
        });
    });
    
    // Botões de excluir quarto
    document.querySelectorAll('.delete-room-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const roomId = btn.getAttribute('data-room-id');
            const roomNumber = btn.getAttribute('data-room-number');
            confirmDelete(roomId, roomNumber);
        });
    });
}

document.addEventListener('DOMContentLoaded', function() {
    if (!checkAuth()) return;

    loadUserInfo();

    loadRooms();

    document.getElementById('addRoomBtn').addEventListener('click', openAddModal);

    document.getElementById('filterAvailableBtn').addEventListener('click', () => filterRooms('available'));
    document.getElementById('filterOccupiedBtn').addEventListener('click', () => filterRooms('occupied'));
    document.getElementById('showAllBtn').addEventListener('click', () => filterRooms('all'));

    document.getElementById('refreshBtn').addEventListener('click', loadRooms);

    document.getElementById('sortRoomsBtn').addEventListener('click', toggleSortOrder);

    document.getElementById('addBedBtn').addEventListener('click', addBed);
    document.getElementById('removeBedBtn').addEventListener('click', removeBed);
    document.getElementById('closeBedModalBtn').addEventListener('click', () => closeModal(bedModal));

    document.getElementById('logoutBtn').addEventListener('click', (e) => {
        e.preventDefault();
        logout();
    });

    document.querySelectorAll('.close').forEach(closeBtn => {
        closeBtn.addEventListener('click', function() {
            const modal = this.closest('.modal');
            closeModal(modal);
        });
    });

    document.getElementById('cancelBtn').addEventListener('click', () => closeModal(roomModal));
    document.getElementById('cancelDeleteBtn').addEventListener('click', () => closeModal(confirmModal));

    window.addEventListener('click', function(event) {
        if (event.target === roomModal) {
            closeModal(roomModal);
        }
        if (event.target === bedModal) {
            closeModal(bedModal);
        }
        if (event.target === confirmModal) {
            closeModal(confirmModal);
        }
    });

    roomForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        if (!checkAuth()) return;

        const roomId = document.getElementById('roomId').value;
        
        const roomNumber = parseInt(document.getElementById('roomNumber').value);
        const roomType = document.getElementById('roomType').value;
        const roomStatus = document.getElementById('roomStatus').value;
        const roomPrice = parseFloat(document.getElementById('roomPrice').value);

        if (!roomNumber || !roomType || !roomStatus || isNaN(roomPrice)) {
            showAlert('Por favor, preencha todos os campos obrigatórios!', 'error');
            return;
        }

        const roomData = {
            number: roomNumber,
            roomType: roomType,
            roomStatus: roomStatus,
            price: roomPrice
        };

        console.log('Enviando dados:', roomData);

        let result;
        if (roomId) {
            result = await apiRequest(`${API_BASE_URL}/update/${roomId}`, {
                method: 'PUT',
                body: JSON.stringify(roomData)
            });
        } else {
            result = await apiRequest(`${API_BASE_URL}/insert`, {
                method: 'POST',
                body: JSON.stringify(roomData)
            });
        }

        if (result) {
            showAlert(roomId ? 'Quarto atualizado com sucesso!' : 'Quarto adicionado com sucesso!');
            closeModal(roomModal);
            loadRooms();
        }
    });
});