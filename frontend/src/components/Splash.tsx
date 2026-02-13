import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import {
  ArrowUpRight,
  Check,
  ChevronDown,
  Search,
  X,
} from "lucide-react";
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
  hidden: { opacity: 0, y: 70 },
  show: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.7, ease: "easeOut" },
  },
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
    <div className="min-h-screen bg-[#ececec] text-[#121212] antialiased">
      {/* HEADER */}
      <header className="bg-[#270049] px-6 py-4 text-white shadow-xl">
        <div className="mx-auto flex w-full max-w-7xl items-center justify-between">
          <div className="flex items-center gap-3 text-2xl font-semibold">
            <img src={logo} className="h-8 w-8 rounded-sm" alt="Second Brain Logo" />
            <span className="select-none">Second Brain</span>
          </div>

          <nav className="hidden gap-10 text-lg md:flex">
            {navItems.map((item) => (
              <span key={item} className="cursor-pointer select-none text-white/90 hover:text-white">
                {item}
              </span>
            ))}
          </nav>

          <button
            type="button"
            onClick={() => navigate("/onboarding")}
            className="select-none rounded-2xl bg-[#8a57ff] px-8 py-3 text-lg font-semibold hover:opacity-90"
          >
            Get Started
          </button>
        </div>
      </header>

      <main className="mx-auto w-full max-w-7xl px-6 pb-32 pt-16">
        {/* HERO */}
        <section className="text-center">
          <motion.div
            initial={{ opacity: 0, y: -40 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="mx-auto mb-6 inline-flex items-center gap-2 rounded-full bg-white px-5 py-2 text-sm text-[#5b1ecf] select-none"
          >
            <span>🧠 Build your memory graph with AI</span>
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 70 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1, duration: 0.7 }}
            className="text-6xl font-bold select-none md:text-7xl"
          >
            Your Second Brain
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 40 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2, duration: 0.7 }}
            className="mx-auto mt-5 max-w-3xl text-lg text-black/60 select-none md:text-xl"
          >
            Capture ideas, connect notes and explore your knowledge graph in one calm,
            intelligent workspace.
          </motion.p>

          <motion.button
            type="button"
            onClick={() => navigate("/onboarding")}
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3, duration: 0.7 }}
            className="mt-8 select-none rounded-2xl bg-[#7b45e1] px-10 py-4 text-lg font-semibold text-white"
          >
            Get Started
          </motion.button>
        </section>

        {/* PREVIEW PANEL */}
        <motion.section
          className="mt-12"
          initial={{ opacity: 0, y: 90 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7 }}
        >
          <motion.div
            animate={{ y: [0, -8, 0] }}
            transition={{ duration: 7, repeat: Infinity, ease: "easeInOut" }}
            className="overflow-hidden rounded-[28px] bg-white shadow-[0_0_80px_12px_rgba(124,68,255,0.85)]"
            style={{ boxShadow: '0 0 80px 12px rgba(124,68,255,0.85), 0 0 0 4px #8b4dff' }}
          >
            <div className="grid min-h-[520px] md:grid-cols-12">
              <aside className="bg-[#2b0054] p-5 text-white md:col-span-3">
                <button className="mb-5 w-full select-none rounded-2xl bg-[#8f63f8] px-4 py-3 text-left text-lg font-semibold">
                  + New Note
                </button>
                <div className="space-y-2 select-none text-white/90">
                  <p>🧠 Brain map</p>
                  <p>📝 Notes</p>
                  <p>📚 Knowledge</p>
                  <p>🔗 Connections</p>
                  <p>📈 Insights</p>
                </div>
              </aside>

              <div className="bg-[#efe8fb] p-5 md:col-span-9">
                <div className="mb-6 flex justify-between">
                  <div className="flex items-center gap-2 select-none rounded-xl bg-white px-4 py-3 text-black/50">
                    <Search className="h-4 w-4" />
                    Search notes, chunks, ideas...
                  </div>
                  <div className="flex items-center gap-2 select-none rounded-xl bg-white px-4 py-3">
                    <img src={logo} className="h-6 w-6 rounded-full" alt="Profile" />
                    You <ChevronDown className="h-4 w-4" />
                  </div>
                </div>

                <div className="relative min-h-[300px] rounded-2xl bg-[#f8f4ff] p-5">
                  {feedNodes.map((node, i) => (
                    <motion.div
                      key={node.title}
                      initial={{ scale: 0.8, opacity: 0 }}
                      whileInView={{ scale: 1, opacity: 1 }}
                      transition={{ delay: i * 0.08 }}
                      className={`absolute ${node.x} ${node.y} select-none rounded-full px-4 py-2 text-sm text-white ${
                        i % 2 === 0 ? "bg-[#3d82f3]" : "bg-[#16b38b]"
                      }`}
                    >
                      {node.title}
                    </motion.div>
                  ))}
                </div>
              </div>
            </div>
          </motion.div>
        </motion.section>

        {/* INTEGRATIONS SECTION */}
        <motion.section
          variants={revealUp}
          initial="hidden"
          whileInView="show"
          viewport={{ once: true }}
          className="mt-24 rounded-3xl bg-white p-10"
        >
          <div className="grid items-center gap-10 md:grid-cols-2">
            <div>
              <span className="inline-block select-none rounded-full bg-[#f2e9ff] px-4 py-2 text-sm text-[#6f36d8]">
                Note Integrations
              </span>

              <h2 className="mt-4 select-none text-5xl font-bold">
                Bring every notes platform into Second Brain
              </h2>

              <p className="mt-4 select-none text-lg text-black/60">
                Connect your note-taking apps in one place. Everything remains searchable,
                linked and useful.
              </p>

              <button
                onClick={() => navigate("/onboarding")}
                className="mt-8 select-none rounded-2xl bg-[#7b45e1] px-8 py-3 text-lg font-semibold text-white"
              >
                Get Started
              </button>
            </div>

            <div className="rounded-2xl bg-[#f6f6f6] p-6">
              <div className="space-y-3">
                {notePlatforms.map((platform) => (
                  <div key={platform.label} className="flex items-center gap-3 select-none rounded-xl bg-white px-4 py-3 text-lg">
                    <span>{platform.icon}</span>
                    <span>{platform.label}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </motion.section>

        {/* WHY SECTION */}
        <motion.section
          variants={revealUp}
          initial="hidden"
          whileInView="show"
          viewport={{ once: true }}
          className="mt-20"
        >
          <div className="text-center mb-10">
            <span className="inline-block select-none rounded-full bg-[#f2e9ff] px-5 py-2 text-lg text-[#6f36d8]">
              Why Second Brain
            </span>
          </div>

          <div className="mx-auto grid max-w-6xl overflow-hidden rounded-3xl bg-white md:grid-cols-2">
            <div className="p-10">
              <h3 className="text-4xl font-bold select-none">Other Note Apps</h3>
              <div className="mt-8 space-y-4 text-xl">
                {oldAppsPain.map((item) => (
                  <div key={item} className="flex gap-3 select-none">
                    <X className="h-5 w-5 flex-shrink-0" /> {item}
                  </div>
                ))}
              </div>
            </div>

            <div className="bg-[#2b0054] p-10 text-white">
              <h3 className="text-4xl font-bold mb-6 select-none">Second Brain</h3>
              <div className="space-y-4 text-xl">
                {secondBrainWins.map((item) => (
                  <div key={item} className="flex gap-3 select-none">
                    <Check className="h-5 w-5 flex-shrink-0" /> {item}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </motion.section>
      </main>

      {/* FOOTER */}
      <footer className="bg-[#270049] px-8 py-16 text-white">
        <div className="mx-auto grid w-full max-w-7xl gap-10 md:grid-cols-4">
          <div>
            <div className="mb-3 flex items-center gap-3 text-3xl font-semibold select-none">
              <img src={logo} className="h-9 w-9 rounded-sm" alt="Second Brain Logo" />
              <span>Second Brain</span>
            </div>
            <p className="text-lg text-white/85 select-none">Your personal AI knowledge OS</p>
            <button
              onClick={() => navigate("/onboarding")}
              className="mt-5 select-none rounded-2xl bg-[#8a57ff] px-8 py-3 text-lg font-semibold"
            >
              Get Started
            </button>
          </div>

          <div className="text-lg text-white/80 select-none">
            <p className="mb-4 text-2xl font-semibold text-white">Sections</p>
            <p>Features</p>
            <p>Integrations</p>
            <p>Knowledge Graph</p>
            <p>FAQ</p>
          </div>

          <div className="text-lg text-white/80 select-none">
            <p className="mb-4 text-2xl font-semibold text-white">Platforms</p>
            <p>Notion</p>
            <p>Google Docs</p>
            <p>Apple Notes</p>
            <p>Obsidian</p>
          </div>

          <div className="text-right text-base text-white/70 select-none md:self-end">
            Privacy Policy
          </div>
        </div>

        <div className="mx-auto mt-10 w-full max-w-7xl pt-5 text-base text-white/70 select-none">
          All Rights Reserved © 2025 Second Brain
        </div>
      </footer>

      {/* FLOATING BUTTON */}
      <motion.button
        type="button"
        onClick={() => navigate("/onboarding")}
        whileHover={{ scale: 1.05 }}
        className="fixed bottom-7 right-7 flex items-center gap-2 select-none rounded-full bg-[#7b45e1] px-6 py-4 text-white shadow-xl"
      >
        Let&apos;s talk <ArrowUpRight className="h-5 w-5" />
      </motion.button>
    </div>
  );
};

export default Splash;