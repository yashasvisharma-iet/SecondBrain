import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowUpRight, Check, ChevronDown, Search, X } from "lucide-react";
import logo from "@/assets/logo.png";

const navItems = ["Product", "FAQ", "Integrations", "About us"];

const notePlatforms = [
  { icon: "📝", label: "Notion" },
  { icon: "📄", label: "Google Docs" },
  { icon: "🍎", label: "Apple Notes" },
  { icon: "📘", label: "OneNote" },
  { icon: "🔷", label: "Obsidian" },
];

const feedNodes = [
  { title: "Welcome", x: "left-8", y: "top-40" },
  { title: "Chunk: Welcome", x: "left-40", y: "top-24" },
  { title: "Chunk: MVP Architecture", x: "left-56", y: "top-8" },
  { title: "MVP Architecture", x: "left-[55%]", y: "top-2" },
];

const revealUp = {
  hidden: { opacity: 0, y: 40 },
  show: { opacity: 1, y: 0 },
};

const oldAppsPain = [
  "Notes scattered across multiple apps",
  "Weak linking between ideas",
  "Search feels slow and fragmented",
  "No unified knowledge graph",
  "Hard to revisit insights quickly",
];

const secondBrainWins = [
  "Everything in one connected workspace",
  "Graph-native note relationships",
  "Fast retrieval with contextual links",
  "AI-assisted thought organization",
  "Clear path from notes to action",
];

const Splash = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-[#ececec] text-[#121212] [&_*]:border-transparent">
      <header className="bg-[#270049] px-6 py-4 text-white shadow-xl">
        <div className="mx-auto flex w-full max-w-7xl items-center justify-between">
          <div className="flex items-center gap-3 text-2xl font-semibold">
            <img src={logo} alt="Second Brain" className="h-8 w-8 rounded-sm" />
            <span>Second Brain</span>
          </div>

          <nav className="hidden gap-10 text-lg md:flex">
            {navItems.map((item) => (
              <span
                key={item}
                className="cursor-pointer text-white/90 transition hover:text-white"
              >
                {item}
              </span>
            ))}
          </nav>

          <button
            onClick={() => navigate("/onboarding")}
            className="rounded-2xl bg-[#8a57ff] px-8 py-3 text-lg font-semibold transition hover:opacity-90"
          >
            Get Started
          </button>
        </div>
      </header>

      <main className="mx-auto w-full max-w-7xl px-6 pb-32 pt-16">
        <section className="relative text-center">
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="mx-auto mb-6 inline-flex items-center gap-2 rounded-full bg-white/95 px-5 py-2 text-sm text-[#5b1ecf] shadow-sm"
          >
            🧠 Build your memory graph with AI
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1, duration: 0.6 }}
            className="text-6xl font-bold md:text-7xl"
          >
            Your Second Brain
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.18, duration: 0.6 }}
            className="mx-auto mt-5 max-w-3xl text-lg text-black/60 md:text-xl"
          >
            Capture ideas, connect notes and explore your knowledge graph in one calm, intelligent workspace.
          </motion.p>

          <motion.button
            onClick={() => navigate("/onboarding")}
            className="mt-8 rounded-2xl bg-[#7b45e1] px-10 py-4 text-lg font-semibold text-white hover:opacity-90"
          >
            Get Started
          </motion.button>
        </section>
      </main>

      <motion.button
        onClick={() => navigate("/onboarding")}
        className="fixed bottom-7 right-7 flex items-center gap-2 rounded-full bg-[#7b45e1] px-6 py-4 text-lg font-semibold text-white shadow-xl"
      >
        Let&apos;s talk <ArrowUpRight className="h-5 w-5" />
      </motion.button>
    </div>
  );
};

export default Splash;
