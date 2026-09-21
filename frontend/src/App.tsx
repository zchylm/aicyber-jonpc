import { useEffect, useState } from "react";
import AdminPage from "./pages/AdminPage";
import JonPcHeroPage from "./pages/JonPcHeroPage";

function App() {
  const [isAdminRoute, setIsAdminRoute] = useState(() => window.location.hash === "#/admin");

  useEffect(() => {
    const updateRoute = () => setIsAdminRoute(window.location.hash === "#/admin");
    window.addEventListener("hashchange", updateRoute);
    return () => window.removeEventListener("hashchange", updateRoute);
  }, []);

  return isAdminRoute ? <AdminPage /> : <JonPcHeroPage />;
}

export default App;
