import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";

export default function GoogleCallback() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const error = searchParams.get("error");

    if (error) {
      sessionStorage.removeItem("google_docs_connected");
      toast.error(`Google Docs connection failed: ${error}`);
      navigate("/onboarding", { replace: true });
      return;
    }

    sessionStorage.setItem("google_docs_connected", "1");
    sessionStorage.setItem("onboarding_step", "3");
    toast.success("Google Docs connected");
    navigate("/onboarding", { replace: true });
  }, [navigate, searchParams]);

  return <div className="p-8 text-center">Connecting Google Docs…</div>;
}
