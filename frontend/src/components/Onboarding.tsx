import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import {
  ChevronLeft,
  ChevronRight,
  Plug,
  Layers,
  FileText,
} from "lucide-react";
import { toast } from "sonner";
import { NOTION_CLIENT_ID, NOTION_REDIRECT_URI, apiUrl } from "@/lib/config";

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

const NOTION_AUTH_URL = NOTION_CLIENT_ID
  ? `https://api.notion.com/v1/oauth/authorize?client_id=${encodeURIComponent(NOTION_CLIENT_ID)}&response_type=code&owner=user&redirect_uri=${encodeURIComponent(NOTION_REDIRECT_URI)}`
  : "";

type CurrentUser = {
  id: number;
  email: string;
  name: string;
  avatarUrl?: string | null;
};

const Onboarding = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const callbackState = (location.state as { notionConnected?: boolean; proceedToGoogle?: boolean } | null) ?? null;
  const [step, setStep] = useState<number>(() => {
    if (callbackState?.proceedToGoogle) {
      return 3;
    }
    const savedStep = sessionStorage.getItem("onboarding_step");
    const parsedStep = Number(savedStep);
    return [1, 2, 3].includes(parsedStep) ? parsedStep : 1;
  });
  const totalSteps = 3;

  const [selectedApps, setSelectedApps] = useState<string[]>(() => {
    const savedApps = sessionStorage.getItem("onboarding_selected_apps");
    if (!savedApps) return [];
    try {
      const parsed = JSON.parse(savedApps);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  });
  const notionConnected =
    callbackState?.notionConnected || sessionStorage.getItem("notion_connected") === "1";
  const googleDocsConnected = sessionStorage.getItem("google_docs_connected") === "1";
  const [googleDocs, setGoogleDocs] = useState<Array<{ id: string; name: string; modifiedTime: string }>>([]);
  const [selectedGoogleDocIds, setSelectedGoogleDocIds] = useState<string[]>([]);
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [authChecked, setAuthChecked] = useState(false);


  useEffect(() => {
    fetch(apiUrl("/api/me"), { credentials: "include" })
      .then(async (res) => {
        if (res.status === 401) {
          setCurrentUser(null);
          return;
        }
        if (!res.ok) {
          throw new Error("Unable to fetch current user");
        }
        const user = (await res.json()) as CurrentUser;
        setCurrentUser(user);
      })
      .catch(() => {
        setCurrentUser(null);
      })
      .finally(() => {
        setAuthChecked(true);
      });
  }, []);

  useEffect(() => {
    sessionStorage.setItem("onboarding_selected_apps", JSON.stringify(selectedApps));
  }, [selectedApps]);

  useEffect(() => {
    sessionStorage.setItem("onboarding_step", String(step));
  }, [step]);

  useEffect(() => {
    if (callbackState?.notionConnected) {
      sessionStorage.setItem("notion_connected", "1");
    }
  }, [callbackState?.notionConnected]);

  useEffect(() => {
    if (!googleDocsConnected || step !== 3) {
      return;
    }

    fetch(apiUrl("/api/google-docs/list"), {
      credentials: "include",
    })
      .then(async (res) => {
        if (!res.ok) {
          throw new Error("Unable to fetch documents");
        }
        const docs = await res.json();
        setGoogleDocs(Array.isArray(docs) ? docs : []);
      })
      .catch(() => {
        toast.error("Unable to fetch Google Docs. Please reconnect and try again.");
      });
  }, [googleDocsConnected, step]);

  const progress = (step / totalSteps) * 100;

  const toggleApp = (app: string) => {
    setSelectedApps((prev) =>
      prev.includes(app)
        ? prev.filter((a) => a !== app)
        : [...prev, app]
    );
  };

  const handleSkipForNow = () => {
    sessionStorage.setItem("onboarding_deferred", "1");
    navigate("/feed");
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

    if (step === 3 && !googleDocsConnected && selectedApps.includes("Google Docs")) {
      toast.error("Please connect your Google Docs account");
      return;
    }

    if (step < totalSteps) {
      setStep(step + 1);
      return;
    }

    sessionStorage.setItem("onboarding_complete", "1");
    sessionStorage.removeItem("onboarding_deferred");
    navigate("/feed");
  };

  const handleBack = () => step > 1 && setStep(step - 1);

  if (!authChecked) {
    return <div className="min-h-screen flex items-center justify-center">Loading your account…</div>;
  }

  if (!currentUser) {
    return (
      <div className="min-h-screen gradient-subtle flex items-center justify-center px-5">
        <div className="w-full max-w-md glass-strong rounded-2xl p-8 shadow-lg border border-border text-center space-y-4">
          <div className="w-16 h-16 mx-auto rounded-full gradient-primary flex items-center justify-center text-white text-2xl">🧠</div>
          <h2 className="text-2xl font-semibold">Sign in before connecting apps</h2>

          <Button
            className="w-full"
            onClick={() => {
              sessionStorage.setItem("google_oauth_intent", "signin");
              sessionStorage.setItem("oauth_return_step", String(step));
              window.location.href = apiUrl("/oauth2/authorization/google");
            }}
          >
            Sign in with Google
          </Button> 
          <Button variant="ghost" className="w-full" onClick={() => navigate("/")}>
            Back to home
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen gradient-subtle flex flex-col">
      <div className="sticky top-0 z-50 bg-background/80 backdrop-blur-xl border-b">
        <div className="max-w-md mx-auto px-4 py-4 space-y-3">
          <div className="flex items-center justify-between text-sm">
            <span className="font-medium">Workspace Setup</span>
            <Button variant="ghost" className="h-auto px-0 text-sm text-muted-foreground hover:text-foreground" onClick={handleSkipForNow}>
              Skip for now
            </Button>
          </div>
          <div className="flex justify-between text-sm mb-2">
            <span className="font-medium">Connect your tools</span>
            <span className="text-muted-foreground">
              Step {step} of {totalSteps}
            </span>
          </div>
          <Progress value={progress} className="h-2 rounded-full" />
        </div>
      </div>

      <div className="flex-1 flex items-center justify-center px-5 py-8">
        <div className="w-full max-w-md glass-strong rounded-2xl p-6 shadow-lg border border-border overflow-hidden">
          <AnimatePresence mode="wait">
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

                <ol className="list-decimal pl-5 text-sm text-muted-foreground space-y-1">
                  <li>Choose the apps you want to connect.</li>
                  <li>Connect Notion (if selected).</li>
                  <li>Connect Google Docs (if selected).</li>
                </ol>

                <div className="rounded-xl border border-dashed border-border/80 bg-background/60 p-4 text-sm text-muted-foreground">
                  You can skip setup now and finish connecting your apps later from the workspace.
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
                  <ol className="list-decimal text-left pl-5 text-xs text-muted-foreground space-y-1">
                    <li>Click <span className="font-medium">Connect Notion</span>.</li>
                    <li>Authorize this app in the Notion window.</li>
                    <li>Return here and continue.</li>
                  </ol>

                  {notionConnected ? (
                    <Badge className="gradient-primary text-white">
                      Notion Connected
                    </Badge>
                  ) : (
                    <Button
                      className="w-full"
                      onClick={() => {
                        sessionStorage.setItem("onboarding_selected_apps", JSON.stringify(selectedApps));
                        sessionStorage.setItem("onboarding_step", "2");
                        if (!NOTION_AUTH_URL) {
                          toast.error("Missing Notion client configuration");
                          return;
                        }
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

            {step === 3 && (
              <motion.div
                key="step3"
                variants={fadeSlide}
                initial="hidden"
                animate="visible"
                exit="exit"
                transition={{ duration: 0.4 }}
                className="space-y-6"
              >
                <div className="text-center mb-6">
                  <div className="w-16 h-16 mx-auto mb-4 rounded-full gradient-primary flex items-center justify-center">
                    <FileText className="w-8 h-8 text-white" />
                  </div>
                  <h3 className="text-2xl font-semibold mb-1">
                    Connect Google Docs
                  </h3>
                  <p className="text-muted-foreground text-sm">
                    Link Google Docs to import docs and meeting notes
                  </p>
                </div>

                <div className="glass p-5 rounded-xl border text-center space-y-4">
                  <ol className="list-decimal text-left pl-5 text-xs text-muted-foreground space-y-1">
                    <li>Click <span className="font-medium">Connect Google Docs</span>.</li>
                    <li>Sign in and grant Google Docs access.</li>
                    <li>Come back here and click Finish.</li>
                  </ol>

                  {selectedApps.includes("Google Docs") ? (
                    googleDocsConnected ? (
                      <div className="space-y-3 text-left">
                        <Badge className="gradient-primary text-white">
                          Google Docs Connected
                        </Badge>
                        <p className="text-xs text-muted-foreground">
                          Select docs to ingest now (you can add more later).
                        </p>
                        <div className="max-h-52 overflow-y-auto space-y-2 rounded-md border p-3">
                          {googleDocs.length === 0 ? (
                            <p className="text-xs text-muted-foreground">No docs found yet.</p>
                          ) : (
                            googleDocs.map((doc) => (
                              <label key={doc.id} className="flex items-center gap-2 text-xs">
                                <input
                                  type="checkbox"
                                  checked={selectedGoogleDocIds.includes(doc.id)}
                                  onChange={() => {
                                    setSelectedGoogleDocIds((prev) =>
                                      prev.includes(doc.id)
                                        ? prev.filter((id) => id !== doc.id)
                                        : [...prev, doc.id]
                                    );
                                  }}
                                />
                                <span className="truncate">{doc.name}</span>
                              </label>
                            ))
                          )}
                        </div>
                        <Button
                          className="w-full"
                          variant="secondary"
                          disabled={selectedGoogleDocIds.length === 0}
                          onClick={() => {
                            fetch(apiUrl("/api/google-docs/ingest-selected"), {
                              method: "POST",
                              headers: { "Content-Type": "application/json" },
                              credentials: "include",
                              body: JSON.stringify({ docIds: selectedGoogleDocIds }),
                            })
                              .then((res) => {
                                if (!res.ok) {
                                  throw new Error();
                                }
                                toast.success("Selected Google Docs ingested");
                              })
                              .catch(() => {
                                toast.error("Failed to ingest selected docs");
                              });
                          }}
                        >
                          Ingest Selected Docs
                        </Button>
                      </div>
                    ) : (
                      <Button
                        className="w-full"
                        onClick={() => {
                          sessionStorage.setItem("google_oauth_intent", "google_docs");
                          sessionStorage.setItem("oauth_return_step", "3");
                          window.location.href = apiUrl("/oauth2/authorization/google");
                        }}
                      >
                        Connect Google Docs
                      </Button>
                    )
                  ) : (
                    <p className="text-xs text-muted-foreground">
                      Google Docs was not selected in step 1, so this step is optional.
                    </p>
                  )}
                </div>
              </motion.div>
            )}
          </AnimatePresence>

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
