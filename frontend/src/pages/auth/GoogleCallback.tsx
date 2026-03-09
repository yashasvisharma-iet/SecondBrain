import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

export default function GoogleCallback() {
  const navigate = useNavigate();

  useEffect(() => {
    sessionStorage.setItem("google_docs_connected", "1");
    sessionStorage.setItem("onboarding_step", "3");
    toast.success("Google Docs connected");
    navigate("/onboarding", { replace: true });
  }, [navigate]);

  return <div className="p-8 text-center">Connecting Google Docs…</div>;
}
