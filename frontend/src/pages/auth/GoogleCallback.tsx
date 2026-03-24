import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import { apiUrl } from "@/lib/config";

export default function GoogleCallback() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const error = searchParams.get("error");
    const oauthIntent = sessionStorage.getItem("google_oauth_intent");
    const returnStep = sessionStorage.getItem("oauth_return_step") ?? "1";

    if (error) {
      sessionStorage.removeItem("google_oauth_intent");
      sessionStorage.removeItem("oauth_return_step");
      sessionStorage.removeItem("google_docs_connected");
      toast.error(`Google Docs connection failed: ${error}`);
      navigate("/onboarding", { replace: true });
      return;
    }

    fetch(apiUrl("/api/me"), { credentials: "include" })
      .then((res) => {
        if (!res.ok) {
          throw new Error("Authentication did not establish a session");
        }

        if (oauthIntent === "google_docs") {
          sessionStorage.setItem("google_docs_connected", "1");
          sessionStorage.setItem("onboarding_step", "3");
          toast.success("Google Docs connected");
        } else {
          sessionStorage.setItem("onboarding_step", returnStep);
          toast.success("Signed in successfully");
        }

        sessionStorage.removeItem("google_oauth_intent");
        sessionStorage.removeItem("oauth_return_step");
        navigate("/onboarding", { replace: true });
      })
      .catch(() => {
        sessionStorage.removeItem("google_docs_connected");
        sessionStorage.removeItem("google_oauth_intent");
        sessionStorage.removeItem("oauth_return_step");
        toast.error("Sign-in succeeded but no session was created. Check cookie/CORS settings.");
        navigate("/onboarding", { replace: true });
      });
  }, [navigate, searchParams]);

  return <div className="p-8 text-center">Connecting Google Docs…</div>;
}
