import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "./ui/button";
import { Loader2 } from "lucide-react";
import logo from "@/assets/logo.png";

const Splash = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [hasAuth, setHasAuth] = useState(false);

  useEffect(() => {
    // Simulate checking for auth token
    const timer = setTimeout(() => {
      const authToken = localStorage.getItem("auth_token");
      setHasAuth(!!authToken);
      setLoading(false);
    }, 2000);

    return () => clearTimeout(timer);
  }, []);

  // const handleGetStarted = () => {
  //   navigate("/login");
  // };
  const handleGetStarted = () => {
    navigate("/onboarding");
  };


  const handleContinue = () => {
    navigate("/onboarding");
  };

  return (
    <div className="relative min-h-screen flex items-center justify-center overflow-hidden bg-gradient-to-br from-background via-background to-primary/10">
      {/* Ambient blobs */}
      <div className="absolute -top-32 -left-32 w-96 h-96 bg-primary/20 rounded-full blur-3xl animate-pulse" />
      <div className="absolute bottom-[-8rem] right-[-6rem] w-96 h-96 bg-primary-glow/20 rounded-full blur-3xl animate-pulse delay-1000" />

      {/* Card */}
      <div className="relative z-10 w-full max-w-md rounded-2xl border border-white/10 bg-background/70 backdrop-blur-xl shadow-2xl p-10 text-center animate-fade-in-scale">
        
        {/* Logo */}
        <div className="relative mb-8 flex justify-center">
          <div className="absolute inset-0 w-32 h-32 rounded-full bg-primary/20 blur-2xl" />
          <img
            src={logo}
            alt="Second Brain logo"
            className="relative w-28 h-28 rounded-2xl ring-1 ring-white/20 shadow-xl"
          />
        </div>

        {/* Title */}
        <h1 className="text-4xl font-bold mb-3 bg-clip-text text-transparent bg-gradient-to-r from-primary to-primary-glow animate-[gradient_4s_linear_infinite] bg-[length:200%_auto]">
          Second Brain
        </h1>

        {/* Tagline */}
        <p className="text-muted-foreground text-lg mb-10 leading-relaxed">
          Organise everything, <br />
          <span className="text-primary font-medium italic">
            remember anything.
          </span>
        </p>

        {/* Loading / CTA */}
        {loading ? (
          <div className="flex items-center justify-center gap-2 text-muted-foreground">
            <Loader2 className="h-5 w-5 animate-spin" />
            <span className="text-sm">Preparing your workspace…</span>
          </div>
        ) : (
          <div className="space-y-4 animate-fade-in">
            {hasAuth ? (
              <Button
                size="lg"
                onClick={handleContinue}
                className="w-full bg-gradient-to-r from-primary to-primary-glow hover:opacity-90 transition-all shadow-lg"
              >
                Continue to Feed →
              </Button>
            ) : (
              <Button
                size="lg"
                onClick={handleGetStarted}
                className="w-full bg-gradient-to-r from-primary to-primary-glow hover:opacity-90 transition-all shadow-lg"
              >
                Get Started →
              </Button>
            )}
          </div>
        )}

        {/* Footer micro-copy */}
        <p className="mt-8 text-xs text-muted-foreground">
          Your personal AI-powered knowledge space
        </p>

        {/* Screen reader text */}
        <p className="sr-only">
          Second Brain is an AI-powered personal knowledge management system.
        </p>
      </div>
    </div>
  );
};
export default Splash;
