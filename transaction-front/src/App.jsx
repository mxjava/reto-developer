import { useEffect, useState } from 'react';
import { cancelTransaction, clearAccessToken, createTransaction, listTransactions, login, setAccessToken } from './api.js';
import { encryptSecret } from './crypto.js';

function Login({ onSuccess }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  async function submit(event) {
    event.preventDefault();
    setError('');
    try {
      const result = await login({ username, password });
      // El bearer token permanece solo en memoria para reducir exposición ante persistencia del navegador.
      setAccessToken(result.accessToken);
      onSuccess();
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  return <main className="center"><section className="card login-card">
    <h1>Reto Developer</h1><p>Inicio de sesión seguro</p>
    <form onSubmit={submit} autoComplete="on">
      <label>Usuario<input autoComplete="username" value={username} onChange={e => setUsername(e.target.value)} required maxLength={80} /></label>
      <label>Password<input type="password" autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} required maxLength={128} /></label>
      <button>Ingresar</button>{error && <div className="error">{error}</div>}
    </form>
  </section></main>;
}

function Transactions() {
  const [form, setForm] = useState({ operacion: 'venta', importe: '100.00', cliente: '', secreto: '' });
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [data, setData] = useState({ content: [], page: 0, totalPages: 0 });

  async function load(page = 0) {
    try {
      setData(await listTransactions(page, 10, 'id,desc'));
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  useEffect(() => { load(0); }, []);

  async function submit(event) {
    event.preventDefault();
    setError(''); setNotice('');
    try {
      const secreto = await encryptSecret(form.secreto);
      const response = await createTransaction({ ...form, secreto });
      setNotice(`Aprobada · Ref ${response.referencia} · ID ${response.id}`);
      setForm({ ...form, secreto: '' });
      await load(0);
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function cancel(transaction) {
    setError('');
    try {
      const response = await cancelTransaction({ id: transaction.id, referencia: transaction.referencia, estatus: 'cancelar' });
      setNotice(`Transacción ${response.id}: ${response.estatus}`);
      await load(data.page);
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  function logout() {
    clearAccessToken();
    window.location.reload();
  }

  return <main className="app"><header><div><h1>Operaciones</h1><span>Registro y consulta de transacciones</span></div>
    <button className="secondary" onClick={logout}>Salir</button></header>
    <section className="grid"><div className="card"><h2>Registrar operación</h2><form onSubmit={submit}>
      {['operacion', 'importe', 'cliente', 'secreto'].map(key => <label key={key}>{key[0].toUpperCase() + key.slice(1)}
        <input type={key === 'secreto' ? 'password' : 'text'} value={form[key]}
          onChange={e => setForm({ ...form, [key]: e.target.value })} required /></label>)}
      <button>Registrar</button>{notice && <div className="success">{notice}</div>}{error && <div className="error">{error}</div>}
    </form></div>
    <div className="card table-card"><h2>Transacciones</h2><div className="table-wrap"><table><thead><tr>
      <th>ID</th><th>Operación</th><th>Importe</th><th>Cliente</th><th>Referencia</th><th>Estatus</th><th></th>
    </tr></thead><tbody>{data.content.map(t => <tr key={t.id}><td>{t.id}</td><td>{t.operacion}</td>
      <td>${Number(t.importe).toFixed(2)}</td><td>{t.cliente}</td><td>{t.referencia}</td>
      <td><span className={`badge ${t.estatus.toLowerCase()}`}>{t.estatus}</span></td>
      <td>{t.estatus === 'Aprobada' && <button className="danger" onClick={() => cancel(t)}>Cancelar</button>}</td></tr>)}</tbody></table></div>
      <div className="pager"><button disabled={data.first} onClick={() => load(data.page - 1)}>Anterior</button>
        <span>Página {data.page + 1} de {Math.max(data.totalPages, 1)}</span>
        <button disabled={data.last || data.totalPages === 0} onClick={() => load(data.page + 1)}>Siguiente</button></div>
    </div></section></main>;
}

export default function App() {
  const [authenticated, setAuthenticated] = useState(false);
  return authenticated ? <Transactions /> : <Login onSuccess={() => setAuthenticated(true)} />;
}
