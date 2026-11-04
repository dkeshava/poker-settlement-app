import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import GameDetails from "./pages/GameDetails";
import ProtectedRoute from "./components/ProtectedRoute";

const App = () => {

    return (
        <BrowserRouter>
            <Routes>

                <Route path="/login" element={<Login />} />

                <Route path="/register" element={<Register />} />

                <Route element={<ProtectedRoute />}>
                    <Route path="/dashboard" element={<Dashboard />} />
                    <Route path="/games/:gameId" element={<GameDetails />} />
                </Route>

                <Route path="/" element={<Navigate to="/dashboard" replace />} />

                <Route path="*" element={<Navigate to="/" replace />} />

            </Routes>
        </BrowserRouter>
    );
};

export default App;
