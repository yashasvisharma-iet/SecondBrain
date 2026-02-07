import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import {
  ChevronLeft,
  ChevronRight,
  Plug,
  Layers,
} from "lucide-react";
import { toast } from "sonner";

const fadeSlide = {
  hidden: { opacity: 0, x: 40 },
  visible: { opacity: 1, x: 0 },
  exit: { opacity: 0, x: -40 },
};

const NOTE_APPS = [
  "Notion",
  "Google Docs",
  "Apple Notes",
  "Obsidian",
  "Roam Research",
  "Markdown Files",
  "Other",
];

const NOTION_AUTH_URL =
  "https://api.notion.com/v1/oauth/authorize?client_id=2ffd872b-594c-8031-9655-003752eb0403&response_type=code&owner=user&redirect_uri=http%3A%2F%2Flocalhost%3A5173%2Fauth%2Fnotion%2Fcallback";

const Onboarding = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const totalSteps = 2;

  // New states
  const [selectedApps, setSelectedApps] = useState<string[]>([]);
  const [notionConnected, setNotionConnected] = useState(false);

  const progress = (step / totalSteps) * 100;

  const toggleApp = (app: string) => {
    setSelectedApps((prev) =>
      prev.includes(app)
        ? prev.filter((a) => a !== app)
        : [...prev, app]
    );
  };

  const handleNext = () => {
    if (step === 1 && selectedApps.length === 0) {
      toast.error("Please select at least one app");
      return;
    }

    if (step === 2 && !notionConnected && selectedApps.includes("Notion")) {
      toast.error("Please connect your Notion account");
      return;
    }

    if (step < totalSteps) setStep(step + 1);
    else navigate("/feed");
  };

  const handleBack = () => step > 1 && setStep(step - 1);

  return (
    <div className="min-h-screen gradient-subtle flex flex-col">
      {/* Progress */}
      <div className="sticky top-0 z-50 bg-background/80 backdrop-blur-xl border-b">
        <div className="max-w-md mx-auto px-4 py-4">
          <div className="flex justify-between text-sm mb-2">
            <span className="font-medium">Workspace Setup</span>
            <span className="text-muted-foreground">
              Step {step} of {totalSteps}
            </span>
          </div>
          <Progress value={progress} className="h-2 rounded-full" />
        </div>
      </div>

      {/* Main */}
      <div className="flex-1 flex items-center justify-center px-5 py-8">
        <div className="w-full max-w-md glass-strong rounded-2xl p-6 shadow-lg border border-border overflow-hidden">
          <AnimatePresence mode="wait">
            {/* Step 1 — Apps */}
            {step === 1 && (
              <motion.div
                key="step1"
                variants={fadeSlide}
                initial="hidden"
                animate="visible"
                exit="exit"
                transition={{ duration: 0.4 }}
                className="space-y-6"
              >
                <div className="text-center mb-6">
                  <div className="w-16 h-16 mx-auto mb-4 rounded-full gradient-primary flex items-center justify-center">
                    <Layers className="w-8 h-8 text-white" />
                  </div>
                  <h3 className="text-2xl font-semibold mb-1">
                    Notes & Knowledge Apps
                  </h3>
                  <p className="text-muted-foreground text-sm">
                    Select the tools you use for note-taking
                  </p>
                </div>

                <div className="flex flex-wrap gap-2 justify-center">
                  {NOTE_APPS.map((app) => (
                    <Badge
                      key={app}
                      variant={
                        selectedApps.includes(app)
                          ? "default"
                          : "outline"
                      }
                      className="cursor-pointer px-4 py-2 text-sm"
                      onClick={() => toggleApp(app)}
                    >
                      {app}
                    </Badge>
                  ))}
                </div>
              </motion.div>
            )}

            {/* Step 2 — Notion */}
            {step === 2 && (
              <motion.div
                key="step2"
                variants={fadeSlide}
                initial="hidden"
                animate="visible"
                exit="exit"
                transition={{ duration: 0.4 }}
                className="space-y-6"
              >
                <div className="text-center mb-6">
                  <div className="w-16 h-16 mx-auto mb-4 rounded-full gradient-primary flex items-center justify-center">
                    <Plug className="w-8 h-8 text-white" />
                  </div>
                  <h3 className="text-2xl font-semibold mb-1">
                    Connect Notion
                  </h3>
                  <p className="text-muted-foreground text-sm">
                    Sync your Notion workspace
                  </p>
                </div>

                <div className="glass p-5 rounded-xl border text-center space-y-4">
                  {notionConnected ? (
                    <Badge className="gradient-primary text-white">
                      Notion Connected
                    </Badge>
                  ) : (
                    <Button
                      className="w-full"
                      onClick={() => {
                        window.location.href = NOTION_AUTH_URL;
                      }}
                    >
                      Connect Notion
                    </Button>
                  )}

                  <p className="text-xs text-muted-foreground">
                    You’ll be redirected to Notion to authorize access
                  </p>
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Navigation */}
          <div className="flex items-center justify-between mt-8 pt-6 border-t border-border">
            <Button
              variant="ghost"
              onClick={handleBack}
              disabled={step === 1}
              className="gap-2"
            >
              <ChevronLeft className="w-4 h-4" /> Back
            </Button>

            <Button onClick={handleNext} className="gap-2">
              {step === totalSteps ? "Finish" : (
                <>
                  Next <ChevronRight className="w-4 h-4" />
                </>
              )}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Onboarding;
