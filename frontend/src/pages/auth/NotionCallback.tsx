import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";

export default function NotionCallback() {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const code = params.get("code");
    const error = params.get("error");

    if (error) {
      toast.error("Notion authorization failed");
      navigate("/onboarding");
      return;
    }

    if (!code) {
      toast.error("Missing authorization code");
      navigate("/onboarding");
      return; 
    }

    //duplicate post handling 
    const handledKey = `notion_handled_${code}`;
    if (sessionStorage.getItem(handledKey)) {
      // already handled this code — navigate back to onboarding
      navigate("/onboarding", { state: { notionConnected: true } });
      return;
    }
    sessionStorage.setItem(handledKey, "1");

    //call the backend to exchange code for token and save connection
    fetch("http://localhost:8080/api/oauth/notion/callback", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ code }),
    })
      .then(async (res) => {
        if (!res.ok) throw new Error();
        navigate("/feed", {
          state: { notionConnected: true },
        });
      })
      .catch(() => {
        toast.error("Failed to connect Notion");
        navigate("/onboarding");
      });
  }, []);

  return <div className="p-8 text-center">Connecting Notion…</div>;
}
