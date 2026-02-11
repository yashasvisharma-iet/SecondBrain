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
              <span key={item} className="cursor-pointer text-white/90 transition hover:text-white">
                {item}
              </span>
            ))}
          </nav>

          <button
            type="button"
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
            <span>🧠 Build your memory graph with AI</span>
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1, duration: 0.6 }}
            className="text-6xl font-bold tracking-tight md:text-7xl"
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
            type="button"
            onClick={() => navigate("/onboarding")}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.26, duration: 0.6 }}
            className="mt-8 rounded-2xl bg-[#7b45e1] px-10 py-4 text-lg font-semibold text-white transition hover:opacity-90"
          >
            Get Started
          </motion.button>
        </section>

        <motion.section
          className="mt-12 overflow-hidden rounded-[28px] bg-white ring-4 ring-[#8b4dff] shadow-[0_0_70px_10px_rgba(124,68,255,0.85)]"
          variants={revealUp}
          initial="hidden"
          whileInView="show"
          viewport={{ once: true, amount: 0.2 }}
          transition={{ duration: 0.75, ease: "easeOut" }}
        >
          <motion.div
            className="grid min-h-[520px] grid-cols-1 md:grid-cols-12"
            initial={{ opacity: 0, y: 26 }}
            whileInView={{ opacity: 1, y: [0, -14, 0] }}
            viewport={{ once: false, amount: 0.35 }}
            transition={{ duration: 5.2, repeat: Number.POSITIVE_INFINITY, ease: "easeInOut" }}
          >
            <aside className="col-span-1 bg-[#2b0054] p-5 text-white md:col-span-3">
              <div className="mb-5 flex items-center gap-3 text-lg font-semibold">
                <img src={logo} alt="Second Brain" className="h-6 w-6 rounded-sm" />
                <span>Second Brain</span>
              </div>
              <button className="mb-5 w-full rounded-2xl bg-[#8f63f8] px-4 py-3 text-left text-lg font-semibold">
                + New Note
              </button>
              <div className="space-y-2 text-base text-white/90">
                <p>🧠 Brain map</p>
                <p>📝 Notes</p>
                <p>📚 Knowledge</p>
                <p>🔗 Connections</p>
                <p>📈 Insights</p>
              </div>
              <button className="mt-8 w-full rounded-2xl bg-white px-4 py-3 text-left text-lg font-semibold text-[#2b0054]">
                + Create Collection
              </button>
            </aside>

            <div className="col-span-1 bg-[#efe8fb] p-5 md:col-span-9">
              <div className="mb-6 flex items-center justify-between gap-3">
                <div className="flex items-center gap-2 rounded-xl bg-white px-4 py-3 text-sm text-black/50 md:text-xl">
                  <Search className="h-4 w-4 md:h-5 md:w-5" />
                  Search notes, chunks, ideas...
                </div>
                <div className="flex items-center gap-2 rounded-xl bg-white px-4 py-3 text-sm md:text-lg">
                  <img src={logo} alt="user" className="h-6 w-6 rounded-full" />
                  You
                  <ChevronDown className="h-4 w-4" />
                </div>
              </div>

              <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
                <div className="relative min-h-[300px] rounded-2xl bg-[#f8f4ff] p-5 shadow-inner">
                  <div className="absolute left-12 top-20 h-[1px] w-32 rotate-[-35deg] bg-[#98a8c0]" />
                  <div className="absolute left-48 top-16 h-[1px] w-24 rotate-[-20deg] bg-[#98a8c0]" />
                  {feedNodes.map((node, index) => (
                    <motion.div
                      key={node.title}
                      initial={{ scale: 0.7, opacity: 0 }}
                      whileInView={{ scale: 1, opacity: 1 }}
                      viewport={{ once: true }}
                      transition={{ delay: 0.2 + index * 0.08, duration: 0.35 }}
                      className={`absolute ${node.x} ${node.y} rounded-full ${index % 2 === 0 ? "bg-[#3d82f3]" : "bg-[#16b38b]"} px-4 py-2 text-sm text-white shadow`}
                    >
                      {node.title}
                    </motion.div>
                  ))}
                </div>

                <div className="rounded-2xl bg-white/95 p-4 shadow-sm">
                  <p className="mb-3 text-2xl font-semibold">Notes</p>
                  <div className="mb-4 flex gap-2">
                    <span className="rounded-full bg-white/80 px-4 py-1 text-base text-[#006d6d]">General</span>
                    <span className="rounded-full bg-white/70 px-4 py-1 text-base">Ideas</span>
                  </div>
                  <div className="rounded-xl bg-[#eefaf8] p-3 text-lg">
                    <p className="font-semibold">MVP Architecture</p>
                    <p className="text-black/60">Graph for navigation, Postgres for raw content, Neo4j for relationships.</p>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        </motion.section>

        <motion.section
          className="mt-24 rounded-3xl bg-white p-10"
          variants={revealUp}
          initial="hidden"
          whileInView="show"
          viewport={{ once: true, amount: 0.2 }}
          transition={{ duration: 0.7, ease: "easeOut" }}
        >
          <div className="grid items-center gap-10 md:grid-cols-2">
            <div>
              <span className="inline-block rounded-full bg-[#f2e9ff] px-4 py-2 text-sm text-[#6f36d8]">Note Integrations</span>
              <h2 className="mt-4 text-5xl font-bold">Bring every notes platform into Second Brain</h2>
              <p className="mt-4 max-w-2xl text-lg text-black/60">
                Connect your note-taking apps in one place. Everything remains searchable, linked and useful.
              </p>
              <button
                type="button"
                onClick={() => navigate("/onboarding")}
                className="mt-8 rounded-2xl bg-[#7b45e1] px-8 py-3 text-lg font-semibold text-white"
              >
                Get Started
              </button>
            </div>

            <div className="rounded-2xl bg-[#f6f6f6] p-6">
              <div className="space-y-3">
                {notePlatforms.map((platform, index) => (
                  <motion.div
                    key={platform.label}
                    initial={{ opacity: 0, x: 20 }}
                    whileInView={{ opacity: 1, x: 0 }}
                    viewport={{ once: true }}
                    transition={{ delay: 0.09 * index, duration: 0.35 }}
                    className="flex items-center gap-3 rounded-xl bg-white px-4 py-3 text-lg shadow-sm"
                  >
                    <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-[#efefef] text-lg">
                      {platform.icon}
                    </span>
                    <span>{platform.label}</span>
                  </motion.div>
                ))}
              </div>
            </div>
          </div>
        </motion.section>

        <motion.section
          className="mt-20"
          variants={revealUp}
          initial="hidden"
          whileInView="show"
          viewport={{ once: true, amount: 0.2 }}
          transition={{ duration: 0.7, ease: "easeOut" }}
        >
          <div className="text-center">
            <span className="inline-block rounded-full bg-[#f2e9ff] px-5 py-2 text-lg text-[#6f36d8]">Why Second Brain</span>
            <h2 className="mx-auto mt-5 max-w-4xl text-6xl font-bold leading-tight">A smarter way to manage your knowledge.</h2>
            <p className="mx-auto mt-4 max-w-2xl text-xl text-black/60">
              Stop context switching between disconnected tools and build one reliable system for your ideas.
            </p>
          </div>

          <div className="mx-auto mt-10 grid max-w-6xl gap-0 overflow-hidden rounded-3xl bg-white shadow-lg md:grid-cols-2">
            <div className="p-10">
              <h3 className="text-5xl font-bold">Other Note Apps</h3>
              <div className="mt-8 space-y-4 text-2xl text-black/65">
                {oldAppsPain.map((item) => (
                  <div key={item} className="flex items-start gap-3">
                    <span className="mt-1 flex h-7 w-7 items-center justify-center rounded-full bg-black/10">
                      <X className="h-4 w-4" />
                    </span>
                    <p>{item}</p>
                  </div>
                ))}
              </div>
            </div>

            <div className="bg-[#2b0054] p-10 text-white shadow-[0_0_50px_6px_rgba(124,68,255,0.75)_inset]">
              <div className="mb-6 flex items-center gap-3 text-4xl font-bold">
                <img src={logo} alt="Second Brain" className="h-10 w-10 rounded-sm" />
                <span>Second Brain</span>
              </div>
              <div className="space-y-4 text-2xl">
                {secondBrainWins.map((item) => (
                  <div key={item} className="flex items-start gap-3">
                    <span className="mt-1 flex h-7 w-7 items-center justify-center rounded-full bg-[#8a57ff]">
                      <Check className="h-4 w-4" />
                    </span>
                    <p>{item}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </motion.section>
      </main>

      <footer className="bg-[#270049] px-8 py-16 text-white">
        <div className="mx-auto grid w-full max-w-7xl gap-10 md:grid-cols-4">
          <div>
            <div className="mb-3 flex items-center gap-3 text-3xl font-semibold">
              <img src={logo} alt="Second Brain" className="h-9 w-9 rounded-sm" />
              <span>Second Brain</span>
            </div>
            <p className="text-lg text-white/85">Your personal AI knowledge OS</p>
            <button
              type="button"
              onClick={() => navigate("/onboarding")}
              className="mt-5 rounded-2xl bg-[#8a57ff] px-8 py-3 text-lg font-semibold"
            >
              Get Started
            </button>
          </div>

          <div className="text-lg text-white/80">
            <p className="mb-4 text-2xl font-semibold text-white">Sections</p>
            <p>Features</p>
            <p>Integrations</p>
            <p>Knowledge Graph</p>
            <p>FAQ</p>
          </div>

          <div className="text-lg text-white/80">
            <p className="mb-4 text-2xl font-semibold text-white">Platforms</p>
            <p>Notion</p>
            <p>Google Docs</p>
            <p>Apple Notes</p>
            <p>Obsidian</p>
          </div>

          <div className="text-right text-base text-white/70 md:self-end">Privacy Policy</div>
        </div>

        <div className="mx-auto mt-10 w-full max-w-7xl  pt-5 text-base text-white/70">
          All Rights Reserved © 2025 Second Brain
        </div>
      </footer>

      <motion.button
        type="button"
        onClick={() => navigate("/onboarding")}
        whileHover={{ scale: 1.04 }}
        whileTap={{ scale: 0.96 }}
        className="fixed bottom-7 right-7 flex items-center gap-2 rounded-full bg-[#7b45e1] px-6 py-4 text-lg font-semibold text-white shadow-xl"
      >
        Let&apos;s talk <ArrowUpRight className="h-5 w-5" />
      </motion.button>
    </div>
  );
};

export default Splash;
