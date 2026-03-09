import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

export default function GoogleCallback() {
  const navigate = useNavigate();

  useEffect(() => {
    sessionStorage.setItem("google_docs_connected", "1");
    toast.success("Google Docs connected");
    navigate("/onboarding");
  }, [navigate]);

  return <div className="p-8 text-center">Connecting Google Docs…</div>;
}
