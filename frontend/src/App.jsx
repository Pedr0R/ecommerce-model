import React, { useState, useEffect } from 'react';

const USERS_API = 'http://localhost:8085/users';
const CATALOGO_API = 'http://localhost:8084/produtos';
const PEDIDOS_API = 'http://localhost:8081/pedidos';

function App() {
  const [activeTab, setActiveTab] = useState('comprar');
  const [theme, setTheme] = useState('dark');

  const toggleTheme = () => {
    const nextTheme = theme === 'dark' ? 'light' : 'dark';
    setTheme(nextTheme);
    if (nextTheme === 'light') {
      document.body.classList.add('light-mode');
    } else {
      document.body.classList.remove('light-mode');
    }
  };
  
  // Data States
  const [users, setUsers] = useState([]);
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  
  // Service Statuses
  const [apiStatus, setApiStatus] = useState({
    users: false,
    catalogo: false,
    pedidos: false
  });

  // Forms States
  const [userForm, setUserForm] = useState({ name: '', email: '', password: '' });
  const [productForm, setProductForm] = useState({ nome: '', descricao: '', preco: '', categoria: '', estoque: '' });
  
  // Purchase State
  const [selectedUser, setSelectedUser] = useState('');
  const [cart, setCart] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState('');
  const [quantity, setQuantity] = useState(1);
  
  // Search state
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');

  // Notifications
  const [message, setMessage] = useState({ text: '', type: '' });

  // Fetch initial data
  const fetchData = async () => {
    // Check Users API
    try {
      const res = await fetch(USERS_API);
      if (res.ok) {
        const data = await res.json();
        setUsers(data);
        setApiStatus(prev => ({ ...prev, users: true }));
      } else {
        setApiStatus(prev => ({ ...prev, users: false }));
      }
    } catch {
      setApiStatus(prev => ({ ...prev, users: false }));
    }

    // Check Catalogo API
    try {
      const res = await fetch(CATALOGO_API);
      if (res.ok) {
        const data = await res.json();
        setProducts(data);
        setApiStatus(prev => ({ ...prev, catalogo: true }));
      } else {
        setApiStatus(prev => ({ ...prev, catalogo: false }));
      }
    } catch {
      setApiStatus(prev => ({ ...prev, catalogo: false }));
    }

    // Check Pedidos API
    try {
      const res = await fetch(PEDIDOS_API);
      if (res.ok) {
        const data = await res.json();
        // Sort orders by ID desc
        setOrders(data.sort((a, b) => b.id - a.id));
        setApiStatus(prev => ({ ...prev, pedidos: true }));
      } else {
        setApiStatus(prev => ({ ...prev, pedidos: false }));
      }
    } catch {
      setApiStatus(prev => ({ ...prev, pedidos: false }));
    }
  };

  useEffect(() => {
    fetchData();
    // 2 seconds polling to see RabbitMQ status updates in real-time
    const interval = setInterval(fetchData, 20000);
    return () => clearInterval(interval);
  }, []);

  const showNotification = (text, type = 'success') => {
    setMessage({ text, type });
    setTimeout(() => setMessage({ text: '', type: '' }), 5000);
  };

  // Helper to format validation errors
  const formatErrorMessage = (data, defaultMsg) => {
    if (data && data.errors && Array.isArray(data.errors) && data.errors.length > 0) {
      const details = data.errors.map(err => `${err.field}: ${err.message}`).join(', ');
      return `${data.message || defaultMsg} (${details})`;
    }
    return (data && data.message) || defaultMsg;
  };

  // Form Submissions
  const handleUserSubmit = async (e) => {
    e.preventDefault();
    try {
      const res = await fetch(USERS_API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(userForm)
      });
      const data = await res.json();
      if (res.ok) {
        showNotification('Usuário cadastrado com sucesso!');
        setUserForm({ name: '', email: '', password: '' });
        fetchData();
      } else {
        showNotification(formatErrorMessage(data, 'Erro ao cadastrar usuário'), 'error');
      }
    } catch (err) {
      showNotification('Erro de conexão com o microsserviço de usuários', 'error');
    }
  };

  const handleProductSubmit = async (e) => {
    e.preventDefault();
    try {
      const res = await fetch(CATALOGO_API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...productForm,
          preco: parseFloat(productForm.preco),
          estoque: parseInt(productForm.estoque)
        })
      });
      const data = await res.json();
      if (res.ok) {
        showNotification('Produto cadastrado com sucesso!');
        setProductForm({ nome: '', descricao: '', preco: '', categoria: '', estoque: '' });
        fetchData();
      } else {
        showNotification(formatErrorMessage(data, 'Erro ao cadastrar produto'), 'error');
      }
    } catch (err) {
      showNotification('Erro de conexão com o microsserviço de catálogo', 'error');
    }
  };

  // Cart Management
  const addToCart = () => {
    if (!selectedProduct) return;
    const prod = products.find(p => p.id === selectedProduct);
    if (!prod) return;

    if (prod.estoque < quantity) {
      showNotification(`Estoque insuficiente no catálogo! (Estoque atual: ${prod.estoque})`, 'error');
      return;
    }

    const existing = cart.find(item => item.produtoId === selectedProduct);
    if (existing) {
      const newQty = existing.quantidade + quantity;
      if (prod.estoque < newQty) {
        showNotification(`A quantidade total ultrapassa o estoque!`, 'error');
        return;
      }
      setCart(cart.map(item => item.produtoId === selectedProduct ? { ...item, quantidade: newQty } : item));
    } else {
      setCart([...cart, { produtoId: selectedProduct, nome: prod.nome, preco: prod.preco, quantidade: quantity }]);
    }
    showNotification('Produto adicionado ao carrinho');
  };

  const removeFromCart = (id) => {
    setCart(cart.filter(item => item.produtoId !== id));
  };

  const executeCheckout = async () => {
    if (!selectedUser) {
      showNotification('Selecione um usuário para realizar a compra!', 'error');
      return;
    }
    if (cart.length === 0) {
      showNotification('O carrinho está vazio!', 'error');
      return;
    }

    const payload = {
      usuarioId: parseInt(selectedUser),
      itens: cart.map(item => ({
        produtoId: item.produtoId,
        quantidade: item.quantidade
      }))
    };

    try {
      const res = await fetch(PEDIDOS_API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = await res.json();
      if (res.ok) {
        showNotification(`Pedido #${data.id} criado com sucesso! Status: AGUARDANDO_PAGAMENTO.`);
        setCart([]);
        fetchData();
      } else {
        showNotification(formatErrorMessage(data, 'Erro ao realizar checkout'), 'error');
      }
    } catch (err) {
      showNotification('Erro de conexão com o microsserviço de pedidos', 'error');
    }
  };

  const simulateStatusChange = async (id, status) => {
    try {
      const res = await fetch(`${PEDIDOS_API}/${id}/status?status=${status}`, {
        method: 'PATCH'
      });
      if (res.ok) {
        showNotification(`Status do Pedido #${id} alterado para ${status}!`);
        fetchData();
      } else {
        const data = await res.json();
        showNotification(data.message || 'Erro ao alterar status', 'error');
      }
    } catch (err) {
      showNotification('Erro de conexão ao alterar status', 'error');
    }
  };

  // Filters for Catalog Search
  const filteredProducts = products.filter(p => {
    const matchesSearch = p.nome.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          p.descricao.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = selectedCategory ? p.categoria === selectedCategory : true;
    return matchesSearch && matchesCategory;
  });

  const categories = [...new Set(products.map(p => p.categoria))];

  return (
    <div className="app-container">
      <header className="app-header">
        <div>
          <h1 className="brand-title">
            E-Commerce Portal <span className="brand-badge">microservices</span>
          </h1>
        </div>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <button 
            className="btn btn-secondary btn-sm" 
            onClick={toggleTheme}
            style={{ width: 'auto', display: 'inline-flex', alignItems: 'center', gap: '0.4rem', padding: '0.35rem 0.75rem', borderRadius: '8px', border: '1px solid var(--border-color)', backgroundColor: 'var(--bg-secondary)', color: 'var(--text-primary)' }}
          >
            {theme === 'dark' ? '☀️ Light Mode' : '🌙 Dark Mode'}
          </button>

          <div className="api-statuses">
            <div className="status-indicator">
              <span className={`status-dot ${apiStatus.users ? 'online' : 'offline'}`}></span>
              Users [PORTA:(8085)]
            </div>
            <div className="status-indicator">
              <span className={`status-dot ${apiStatus.catalogo ? 'online' : 'offline'}`}></span>
              Catalog [PORTA:(8084)]
            </div>
            <div className="status-indicator">
              <span className={`status-dot ${apiStatus.pedidos ? 'online' : 'offline'}`}></span>
              Orders [PORTA:(8081)]
            </div>
          </div>
        </div>
      </header>

      {message.text && (
        <div className={`message message-${message.type}`}>
          {message.text}
        </div>
      )}

      <div className="tabs">
        <button className={`tab-btn ${activeTab === 'comprar' ? 'active' : ''}`} onClick={() => setActiveTab('comprar')}>
          🛒 Comprar / Checkout
        </button>
        <button className={`tab-btn ${activeTab === 'produtos' ? 'active' : ''}`} onClick={() => setActiveTab('produtos')}>
          📦 Catálogo (Admin)
        </button>
        <button className={`tab-btn ${activeTab === 'usuarios' ? 'active' : ''}`} onClick={() => setActiveTab('usuarios')}>
          👥 Usuários (Admin)
        </button>
      </div>

      <div className="dashboard-grid">
        
        {/* SIDEBAR FOR ACTIONS */}
        <aside className="sidebar">
          {activeTab === 'comprar' && (
            <div className="panel">
              <h3 className="panel-title">Fazer Pedido</h3>
              
              <div className="form-group">
                <label>Selecionar Cliente</label>
                <select 
                  className="form-control" 
                  value={selectedUser} 
                  onChange={(e) => setSelectedUser(e.target.value)}
                >
                  <option value="">Selecione um cliente...</option>
                  {users.map(u => (
                    <option key={u.id} value={u.id}>{u.name} ({u.email})</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Selecionar Produto</label>
                <select 
                  className="form-control" 
                  value={selectedProduct} 
                  onChange={(e) => setSelectedProduct(e.target.value)}
                >
                  <option value="">Selecione um produto...</option>
                  {products.map(p => (
                    <option key={p.id} value={p.id}>{p.nome} - R$ {p.preco.toFixed(2)} (Estoque: {p.estoque})</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Quantidade</label>
                <input 
                  type="number" 
                  className="form-control" 
                  min="1" 
                  value={quantity} 
                  onChange={(e) => setQuantity(parseInt(e.target.value))} 
                />
              </div>

              <button className="btn btn-primary" onClick={addToCart} disabled={!selectedProduct}>
                Adicionar ao Carrinho
              </button>

              {cart.length > 0 && (
                <div className="cart-items">
                  <h4 style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>Itens no Carrinho</h4>
                  {cart.map(item => (
                    <div key={item.produtoId} className="cart-item">
                      <div>
                        {item.nome} (x{item.quantidade})
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <span>R$ {(item.preco * item.quantidade).toFixed(2)}</span>
                        <button 
                          className="btn btn-secondary btn-sm" 
                          style={{ padding: '0.1rem 0.3rem', borderRadius: '4px', border: 'none' }}
                          onClick={() => removeFromCart(item.produtoId)}
                        >
                          ✕
                        </button>
                      </div>
                    </div>
                  ))}
                  <div className="cart-total">
                    <span>Total:</span>
                    <span>R$ {cart.reduce((sum, item) => sum + (item.preco * item.quantidade), 0).toFixed(2)}</span>
                  </div>
                  <button className="btn btn-primary" style={{ marginTop: '1rem', background: 'linear-gradient(135deg, var(--status-success), var(--accent-cyan))' }} onClick={executeCheckout}>
                    Finalizar Compra
                  </button>
                </div>
              )}
            </div>
          )}

          {activeTab === 'produtos' && (
            <div className="panel">
              <h3 className="panel-title">Cadastrar Produto</h3>
              <form onSubmit={handleProductSubmit}>
                <div className="form-group">
                  <label>Nome do Produto</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    required 
                    value={productForm.nome} 
                    onChange={(e) => setProductForm({ ...productForm, nome: e.target.value })} 
                  />
                </div>
                <div className="form-group">
                  <label>Descrição</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    required 
                    value={productForm.descricao} 
                    onChange={(e) => setProductForm({ ...productForm, descricao: e.target.value })} 
                  />
                </div>
                <div className="form-group">
                  <label>Preço (R$)</label>
                  <input 
                    type="number" 
                    step="0.01" 
                    className="form-control" 
                    required 
                    value={productForm.preco} 
                    onChange={(e) => setProductForm({ ...productForm, preco: e.target.value })} 
                  />
                </div>
                <div className="form-group">
                  <label>Categoria</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    required 
                    value={productForm.categoria} 
                    onChange={(e) => setProductForm({ ...productForm, categoria: e.target.value })} 
                  />
                </div>
                <div className="form-group">
                  <label>Estoque Inicial</label>
                  <input 
                    type="number" 
                    className="form-control" 
                    required 
                    value={productForm.estoque} 
                    onChange={(e) => setProductForm({ ...productForm, estoque: e.target.value })} 
                  />
                </div>
                <button type="submit" className="btn btn-primary">Cadastrar no MongoDB</button>
              </form>
            </div>
          )}

          {activeTab === 'usuarios' && (
            <div className="panel">
              <h3 className="panel-title">Cadastrar Usuário</h3>
              <form onSubmit={handleUserSubmit}>
                <div className="form-group">
                  <label>Nome Completo</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    required 
                    value={userForm.name} 
                    onChange={(e) => setUserForm({ ...userForm, name: e.target.value })} 
                  />
                </div>
                <div className="form-group">
                  <label>Endereço de E-mail</label>
                  <input 
                    type="email" 
                    className="form-control" 
                    required 
                    value={userForm.email} 
                    onChange={(e) => setUserForm({ ...userForm, email: e.target.value })} 
                  />
                </div>
                <div className="form-group">
                  <label>Senha de Acesso</label>
                  <input 
                    type="password" 
                    className="form-control" 
                    required 
                    value={userForm.password} 
                    onChange={(e) => setUserForm({ ...userForm, password: e.target.value })} 
                  />
                </div>
                <button type="submit" className="btn btn-primary">Cadastrar no Postgres</button>
              </form>
            </div>
          )}
        </aside>

        {/* MAIN DISPLAY AREA */}
        <main className="main-content">
          
          {activeTab === 'comprar' && (
            <>
              {/* ORDERS TRACKER */}
              <div className="panel">
                <h3 className="panel-title">
                  📦 Rastreabilidade de Pedidos (Processamento RabbitMQ em tempo real)
                  <span style={{ fontSize: '0.75rem', fontWeight: 'normal', color: 'var(--text-secondary)' }}>Atualiza a cada 2s</span>
                </h3>
                <div className="orders-table-wrapper">
                  <table className="orders-table">
                    <thead>
                      <tr>
                        <th>Pedido ID</th>
                        <th>Cliente ID</th>
                        <th>Valor Total</th>
                        <th>Status Atual</th>
                        <th>Ações Simulação (Saga/Estorno)</th>
                      </tr>
                    </thead>
                    <tbody>
                      {orders.map(o => (
                        <tr key={o.id}>
                          <td>#{o.id}</td>
                          <td>Cliente {o.usuarioId}</td>
                          <td style={{ fontWeight: '600' }}>R$ {o.valorTotal.toFixed(2)}</td>
                          <td>
                            <span className={`badge ${
                              o.status === 'PAGO' ? 'badge-paid' :
                              o.status === 'RECUSADO' ? 'badge-failed' : 'badge-pending'
                            }`}>
                              {o.status}
                            </span>
                          </td>
                          <td style={{ display: 'flex', gap: '0.5rem' }}>
                            {o.status === 'AGUARDANDO_PAGAMENTO' && (
                              <>
                                <button className="btn btn-secondary btn-sm" onClick={() => simulateStatusChange(o.id, 'PAGO')}>
                                  Aprovar Pagamento
                                </button>
                                <button className="btn btn-secondary btn-sm" onClick={() => simulateStatusChange(o.id, 'RECUSADO')} style={{ color: 'var(--status-failed)' }}>
                                  Recusar (Estorna Estoque)
                                </button>
                              </>
                            )}
                            {o.status === 'PAGO' && (
                              <button className="btn btn-secondary btn-sm" onClick={() => simulateStatusChange(o.id, 'EM_TRANSPORTE')}>
                                Enviar Mercadoria
                              </button>
                            )}
                            {o.status === 'EM_TRANSPORTE' && (
                              <button className="btn btn-secondary btn-sm" onClick={() => simulateStatusChange(o.id, 'ENTREGUE')}>
                                Marcar como Entregue
                              </button>
                            )}
                            {o.status !== 'AGUARDANDO_PAGAMENTO' && o.status !== 'PAGO' && o.status !== 'EM_TRANSPORTE' && (
                              <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Sem ações pendentes</span>
                            )}
                          </td>
                        </tr>
                      ))}
                      {orders.length === 0 && (
                        <tr>
                          <td colSpan="5" style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: '2rem' }}>
                            Nenhum pedido realizado até o momento.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* PRODUCTS IN COMPRAR VIEW FOR QUICK GLANCE */}
              <div className="panel">
                <h3 className="panel-title">Estoque do Catálogo (MongoDB)</h3>
                <div className="products-grid">
                  {products.map(p => (
                    <div key={p.id} className="product-card">
                      <div>
                        <span className="product-tag">{p.categoria}</span>
                        <h4>{p.nome}</h4>
                        <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>{p.descricao}</p>
                      </div>
                      <div>
                        <div className="product-price">R$ {p.preco.toFixed(2)}</div>
                        <div className="product-stock">
                          <span className={`stock-indicator ${p.estoque <= 2 ? 'stock-low' : 'stock-ok'}`}>
                            ● {p.estoque} em estoque
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}

          {activeTab === 'produtos' && (
            <div className="panel">
              <h3 className="panel-title">Catálogo Completo</h3>
              
              <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem' }}>
                <input 
                  type="text" 
                  placeholder="Pesquisar produtos..." 
                  className="form-control"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
                
                <select 
                  className="form-control"
                  style={{ width: '200px' }}
                  value={selectedCategory}
                  onChange={(e) => setSelectedCategory(e.target.value)}
                >
                  <option value="">Todas as categorias</option>
                  {categories.map(c => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>

              <div className="products-grid">
                {filteredProducts.map(p => (
                  <div key={p.id} className="product-card">
                    <div>
                      <span className="product-tag">{p.categoria}</span>
                      <h4>{p.nome}</h4>
                      <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>{p.descricao}</p>
                      <code style={{ fontSize: '0.65rem', color: 'var(--text-secondary)', display: 'block', marginTop: '0.5rem' }}>ID: {p.id}</code>
                    </div>
                    <div>
                      <div className="product-price">R$ {p.preco.toFixed(2)}</div>
                      <div className="product-stock">
                        <span className={`stock-indicator ${p.estoque <= 2 ? 'stock-low' : 'stock-ok'}`}>
                          ● {p.estoque} unidades
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
                {filteredProducts.length === 0 && (
                  <div style={{ gridColumn: '1/-1', textAlign: 'center', color: 'var(--text-secondary)', padding: '2rem' }}>
                    Nenhum produto correspondente encontrado no MongoDB.
                  </div>
                )}
              </div>
            </div>
          )}

          {activeTab === 'usuarios' && (
            <div className="panel">
              <h3 className="panel-title">Lista de Clientes Cadastrados</h3>
              <div className="item-list" style={{ maxHeight: 'none' }}>
                {users.map(u => (
                  <div key={u.id} className="item-row">
                    <div className="item-info">
                      <h4>{u.name}</h4>
                      <p>{u.email}</p>
                    </div>
                    <span className="brand-badge" style={{ fontSize: '0.65rem' }}>ID #{u.id}</span>
                  </div>
                ))}
                {users.length === 0 && (
                  <div style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: '2rem' }}>
                    Nenhum usuário cadastrado no PostgreSQL.
                  </div>
                )}
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

export default App;
