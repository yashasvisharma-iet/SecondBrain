import { Toaster } from "@/components/ui/sonner"
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "./components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Routes, Route } from "react-router-dom";
// import BottomNav from "./components/BottomNav";
import Splash from "./components/Splash";
// import Login from "./components/Login";
// import Signup from "./components/Signup";
import Feed from "./pages/Feed";
import Onboarding from "./components/Onboarding";
import NotionCallback from "./pages/auth/NotionCallback";

const LayoutWithNav = ({ children }: { children: React.ReactNode }) => (
  <>
    {children}
  </>
);

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      {/* ✅ Removed BrowserRouter here */}
      <Routes>
        <Route path="/auth/notion/callback" element={<NotionCallback />}/>
        <Route path="/" element={<Splash />} />
        {/* <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} /> */}
        <Route path="/onboarding" element={<Onboarding />} />
        <Route
          path="/feed"
          element={
            <LayoutWithNav>
              <Feed />
            </LayoutWithNav>
          }
        />
      </Routes>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
