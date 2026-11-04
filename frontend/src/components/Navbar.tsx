import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
export default function Navbar() { const { logout } = useAuth(); const navigate = useNavigate(); return <header className="navbar"><Link className="brand" to="/dashboard">♠ Ledger</Link><button className="button ghost" onClick={() => { logout(); navigate("/login", { replace: true }); }}>Log out</button></header>; }
