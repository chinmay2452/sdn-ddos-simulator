import React, { useState, useEffect } from 'react';
import { 
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell 
} from 'recharts';
import { 
  Shield, Activity, Server, AlertTriangle, List, Terminal as TerminalIcon, CheckCircle
} from 'lucide-react';
import './App.css';

const App = () => {
  const [data, setData] = useState({
    totalPackets: 0,
    attackDetected: false,
    nodes: [],
    logs: []
  });

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchData = async () => {
    try {
      const response = await fetch('http://localhost:8081/api/network');
      if (!response.ok) throw new Error('Backend server not reachable');
      const json = await response.json();
      setData(json);
      setLoading(false);
      setError(null);
    } catch (err) {
      console.error("Fetch error:", err);
      setError("Waiting for Java Backend to start...");
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 2000); // Poll every 2 seconds
    return () => clearInterval(interval);
  }, []);

  if (loading && !error) {
    return <div className="loading">Connecting to SDN Simulator...</div>;
  }

  return (
    <div className="dashboard-container">
      <header>
        <div>
          <h1>SDN DDoS Detection Dashboard</h1>
          <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>
            {error ? <span style={{color: 'var(--danger)'}}>{error}</span> : "Connected to Live Java Simulation"}
          </p>
        </div>
        <div className={`status-badge ${data.attackDetected ? 'alert' : 'active'}`}>
          {data.attackDetected ? <AlertTriangle size={18} /> : <CheckCircle size={18} />}
          {data.attackDetected ? 'DDoS Attack Detected' : 'System Secure'}
        </div>
      </header>

      {/* Overview Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-label">Total Nodes</div>
          <div className="stat-value">{data.nodes.length}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Active Nodes</div>
          <div className="stat-value" style={{ color: 'var(--success)' }}>
            {data.nodes.filter(n => !n.blocked).length}
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Blocked Nodes</div>
          <div className="stat-value" style={{ color: 'var(--danger)' }}>
            {data.nodes.filter(n => n.blocked).length}
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Total Packets</div>
          <div className="stat-value" style={{ color: 'var(--primary)' }}>
            {data.totalPackets.toLocaleString()}
          </div>
        </div>
      </div>

      <div className="main-grid">
        {/* Traffic Chart */}
        <div className="panel">
          <div className="panel-title">
            <Activity size={20} color="var(--primary)" />
            Traffic Flow Analysis (by IP)
          </div>
          <div style={{ width: '100%', height: 350 }}>
            <ResponsiveContainer>
              <BarChart data={data.nodes}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" />
                <XAxis dataKey="ip" fontSize={10} tickLine={false} axisLine={false} />
                <YAxis fontSize={12} tickLine={false} axisLine={false} />
                <Tooltip 
                  cursor={{ fill: '#f1f5f9' }}
                  contentStyle={{ borderRadius: '8px', border: '1px solid var(--border)', boxShadow: 'var(--shadow)' }}
                />
                <Bar dataKey="packets" radius={[4, 4, 0, 0]}>
                  {data.nodes.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.blocked ? 'var(--danger)' : 'var(--primary)'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Controller Logs */}
        <div className="panel">
          <div className="panel-title">
            <TerminalIcon size={20} color="var(--text-main)" />
            Live System Logs
          </div>
          <div className="terminal">
            {data.logs.length === 0 && <div className="log-entry">Waiting for logs...</div>}
            {data.logs.map((log, i) => (
              <div key={i} className="log-entry">
                <span className="log-time">[{new Date().toLocaleTimeString()}]</span>
                <span className={log.includes("ATTACK") ? "log-type-error" : "log-type-info"}>
                  {log.includes("ATTACK") ? "ALERT" : "INFO"}:
                </span>{' '}
                {log}
              </div>
            ))}
          </div>
        </div>

        {/* Node Table */}
        <div className="panel" style={{ gridColumn: '1 / -1' }}>
          <div className="panel-title">
            <Server size={20} color="var(--primary)" />
            Network Nodes Inventory
          </div>
          <div className="data-table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Node ID</th>
                  <th>IP Address</th>
                  <th>Status</th>
                  <th>Packets Processed</th>
                  <th>Security Status</th>
                </tr>
              </thead>
              <tbody>
                {data.nodes.map((node, i) => (
                  <tr key={i}>
                    <td>{node.id}</td>
                    <td>{node.ip}</td>
                    <td>
                      <span className={`tag ${node.blocked ? 'tag-malicious' : 'tag-normal'}`}>
                        {node.blocked ? 'BLOCKED' : 'ACTIVE'}
                      </span>
                    </td>
                    <td>{node.packets.toLocaleString()}</td>
                    <td>
                      <span style={{ color: node.blocked ? 'var(--danger)' : 'var(--success)', fontWeight: 600 }}>
                        {node.blocked ? 'THREAT ELIMINATED' : 'SECURE'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export default App;
