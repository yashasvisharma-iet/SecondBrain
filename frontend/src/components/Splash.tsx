import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowUpRight, ChevronDown, Instagram, Linkedin, Play, Search, X } from "lucide-react";
import logo from "@/assets/logo.png";

const navItems = ["Product", "FAQ", "Integrations", "About us"];

const integrations = [
  { icon: "🎵", label: "TikTok" },
  { icon: "▶️", label: "YouTube" },
  { icon: "in", label: "LinkedIn" },
  { icon: "𝕏", label: "X" },
  { icon: "📸", label: "Instagram" },
];

const feedNodes = [
  { title: "Welcome", x: "left-8", y: "top-40" },
  { title: "Chunk: Welcome", x: "left-36", y: "top-24" },
  { title: "Chunk: MVP Architecture", x: "left-44", y: "top-6" },
  { title: "MVP Architecture", x: "left-72", y: "top-0" },
];

const revealUp = {
  hidden: { opacity: 0, y: 36 },
  show: { opacity: 1, y: 0 },
};

const Splash = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-[#ececec] text-[#121212]">
      <header className="bg-[#270049] px-6 py-4 text-white shadow-lg">
        <div className="mx-auto flex w-full max-w-6xl items-center justify-between">
          <div className="flex items-center gap-2 text-sm font-semibold">
            <img src={logo} alt="Second Brain" className="h-6 w-6 rounded-sm" />
            <span>Second Brain</span>
          </div>

          <nav className="hidden gap-8 text-xs md:flex">
            {navItems.map((item) => (
              <span key={item} className="cursor-pointer text-white/90 transition hover:text-white">
                {item}
              </span>
            ))}
          </nav>

          <button
            type="button"
            onClick={() => navigate("/onboarding")}
            className="rounded-md bg-[#8a57ff] px-4 py-2 text-xs font-semibold transition hover:opacity-90"
          >
            Get Started
          </button>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl px-6 pb-24 pt-14">
        <section className="relative text-center">
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="mx-auto mb-5 inline-flex items-center gap-2 rounded-full border border-[#a36bff] bg-white px-4 py-1 text-xs text-[#5b1ecf] shadow-sm"
          >
            <span>🧠✨ Build your memory graph with AI</span>
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1, duration: 0.6 }}
            className="text-5xl font-bold tracking-tight"
          >
            Your Second Brain
          </motion.h1>
          <motion.p
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.18, duration: 0.6 }}
            className="mx-auto mt-4 max-w-xl text-sm text-black/60"
          >
            Capture ideas, connect notes and explore your knowledge graph in one calm, intelligent workspace.
          </motion.p>
          <motion.button
            type="button"
            onClick={() => navigate("/onboarding")}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.26, duration: 0.6 }}
            className="mt-5 rounded-md bg-[#7b45e1] px-6 py-2 text-sm font-semibold text-white transition hover:opacity-90"
          >
            Get Started
          </motion.button>

          <motion.div
            animate={{ y: [0, -10, 0], rotate: [-8, -5, -8] }}
            transition={{ duration: 5, repeat: Number.POSITIVE_INFINITY, ease: "easeInOut" }}
            className="pointer-events-none absolute left-0 top-0 hidden -translate-x-8 rounded-xl bg-[#2d7dba] p-3 text-white md:block"
          >
            <Linkedin className="h-8 w-8" />
          </motion.div>
          <motion.div
            animate={{ y: [0, -12, 0], rotate: [8, 5, 8] }}
            transition={{ duration: 6, repeat: Number.POSITIVE_INFINITY, ease: "easeInOut", delay: 0.3 }}
            className="pointer-events-none absolute right-0 top-0 hidden translate-x-8 rounded-xl bg-black p-3 text-white md:block"
          >
            <X className="h-8 w-8" />
          </motion.div>
          <motion.div
            animate={{ y: [0, -10, 0], rotate: [-18, -14, -18] }}
            transition={{ duration: 5.8, repeat: Number.POSITIVE_INFINITY, ease: "easeInOut", delay: 0.2 }}
            className="pointer-events-none absolute left-8 top-28 hidden rounded-xl bg-gradient-to-br from-[#7f39fb] to-[#ff7c45] p-3 text-white md:block"
          >
            <Instagram className="h-8 w-8" />
          </motion.div>
          <motion.div
            animate={{ y: [0, -11, 0], rotate: [18, 14, 18] }}
            transition={{ duration: 6.2, repeat: Number.POSITIVE_INFINITY, ease: "easeInOut", delay: 0.1 }}
            className="pointer-events-none absolute right-8 top-28 hidden rounded-xl bg-[#ff2b2b] p-3 text-white md:block"
          >
            <Play className="h-8 w-8" />
          </motion.div>
        </section>

        <motion.section
          className="mt-10 overflow-hidden rounded-3xl border-4 border-[#8b4dff] bg-white shadow-[0_0_40px_0_rgba(124,68,255,0.5)]"
          variants={revealUp}
          initial="hidden"
          whileInView="show"
          viewport={{ once: true, amount: 0.25 }}
          transition={{ duration: 0.7, ease: "easeOut" }}
        >
          <div className="grid min-h-[460px] grid-cols-1 md:grid-cols-12">
            <aside className="col-span-1 bg-[#2b0054] p-4 text-white md:col-span-3">
              <div className="mb-4 flex items-center gap-2 text-xs font-semibold">
                <img src={logo} alt="Second Brain" className="h-5 w-5 rounded-sm" />
                <span>Second Brain</span>
              </div>
              <button className="mb-4 w-full rounded-md bg-[#8f63f8] px-3 py-2 text-left text-xs font-semibold">
                + New Note
              </button>
              <div className="space-y-2 text-xs text-white/80">
                <p>🧠 Brain map</p>
                <p>📝 Notes</p>
                <p>📚 Knowledge</p>
                <p>🔗 Connections</p>
                <p>📈 Insights</p>
              </div>
              <button className="mt-6 w-full rounded-md bg-white px-3 py-2 text-left text-xs font-semibold text-[#2b0054]">
                + Create Collection
              </button>
            </aside>

            <motion.div
              animate={{ y: [0, -8, 0] }}
              transition={{ duration: 4.5, repeat: Number.POSITIVE_INFINITY, ease: "easeInOut" }}
              className="col-span-1 bg-[#efe8fb] p-4 md:col-span-9"
            >
              <div className="mb-6 flex items-center justify-between gap-3">
                <div className="flex items-center gap-2 rounded-md bg-white px-3 py-2 text-xs text-black/50">
                  <Search className="h-3.5 w-3.5" />
                  Search notes, chunks, ideas...
                </div>
                <div className="flex items-center gap-2 rounded-md bg-white px-3 py-2 text-xs">
                  <img src={logo} alt="user" className="h-5 w-5 rounded-full" />
                  You
                  <ChevronDown className="h-3.5 w-3.5" />
                </div>
              </div>

              <div className="grid gap-6 lg:grid-cols-[1fr_280px]">
                <div className="relative min-h-[250px] rounded-2xl border border-[#d8c8fb] bg-[#f8f4ff] p-5">
                  <div className="absolute left-10 top-14 h-[1px] w-28 rotate-[-35deg] bg-[#98a8c0]" />
                  <div className="absolute left-40 top-10 h-[1px] w-20 rotate-[-20deg] bg-[#98a8c0]" />
                  {feedNodes.map((node, index) => (
                    <motion.div
                      key={node.title}
                      initial={{ scale: 0.7, opacity: 0 }}
                      whileInView={{ scale: 1, opacity: 1 }}
                      viewport={{ once: true }}
                      transition={{ delay: 0.2 + index * 0.08, duration: 0.35 }}
                      className={`absolute ${node.x} ${node.y} rounded-full ${index % 2 === 0 ? "bg-[#3d82f3]" : "bg-[#16b38b]"} px-3 py-2 text-xs text-white shadow`}
                    >
                      {node.title}
                    </motion.div>
                  ))}
                </div>

                <div className="rounded-2xl border border-[#d8d8d8] bg-white p-4">
                  <p className="mb-3 text-sm font-semibold">Notes</p>
                  <div className="mb-4 flex gap-2">
                    <span className="rounded-full border border-[#008080] px-3 py-1 text-xs text-[#006d6d]">General</span>
                    <span className="rounded-full border border-[#b8b8b8] px-3 py-1 text-xs">Ideas</span>
                  </div>
                  <div className="rounded-xl border border-[#008080] bg-[#eefaf8] p-3 text-sm">
                    <p className="font-semibold">MVP Architecture</p>
                    <p className="text-black/60">Graph for navigation, Postgres for raw content, Neo4j for relationships.</p>
                  </div>
                </div>
              </div>
            </motion.div>
          </div>
        </motion.section>

        <motion.section
          className="mt-20 rounded-3xl bg-white p-8"
          variants={revealUp}
          initial="hidden"
          whileInView="show"
          viewport={{ once: true, amount: 0.25 }}
          transition={{ duration: 0.7, ease: "easeOut" }}
        >
          <div className="grid items-center gap-8 md:grid-cols-2">
            <div>
              <span className="inline-block rounded-full border border-[#8b4dff] px-3 py-1 text-xs text-[#6f36d8]">Integrations</span>
              <h2 className="mt-4 text-4xl font-bold">Bring every source into your Second Brain</h2>
              <p className="mt-3 max-w-md text-sm text-black/60">
                Connect social posts, docs and quick notes. Your workspace keeps everything searchable and connected.
              </p>
              <button
                type="button"
                onClick={() => navigate("/onboarding")}
                className="mt-6 rounded-md bg-[#7b45e1] px-5 py-2 text-sm font-semibold text-white"
              >
                Get Started
              </button>
            </div>

            <div className="rounded-2xl bg-[#f6f6f6] p-5">
              <div className="space-y-3">
                {integrations.map((integration, index) => (
                  <motion.div
                    key={integration.label}
                    initial={{ opacity: 0, x: 18 }}
                    whileInView={{ opacity: 1, x: 0 }}
                    viewport={{ once: true }}
                    transition={{ delay: 0.08 * index, duration: 0.35 }}
                    className="flex items-center gap-3 rounded-md bg-white px-3 py-2 text-sm shadow-sm"
                  >
                    <span className="flex h-8 w-8 items-center justify-center rounded-md bg-[#efefef] text-base">
                      {integration.icon}
                    </span>
                    <span>{integration.label}</span>
                  </motion.div>
                ))}
              </div>
            </div>
          </div>
        </motion.section>
      </main>

      <footer className="bg-[#270049] px-6 py-10 text-white">
        <div className="mx-auto grid w-full max-w-6xl gap-8 md:grid-cols-4">
          <div>
            <div className="mb-2 flex items-center gap-2 text-sm font-semibold">
              <img src={logo} alt="Second Brain" className="h-6 w-6 rounded-sm" />
              <span>Second Brain</span>
            </div>
            <p className="text-xs text-white/80">Your personal AI knowledge OS</p>
            <button
              type="button"
              onClick={() => navigate("/onboarding")}
              className="mt-4 rounded-md bg-[#8a57ff] px-4 py-2 text-xs font-semibold"
            >
              Get Started
            </button>
          </div>

          <div className="text-xs text-white/80">
            <p className="mb-3 text-sm font-semibold text-white">Sections</p>
            <p>Features</p>
            <p>Integrations</p>
            <p>Knowledge Graph</p>
            <p>FAQ</p>
          </div>

          <div className="text-xs text-white/80">
            <p className="mb-3 text-sm font-semibold text-white">Socials</p>
            <p>Instagram</p>
            <p>X</p>
            <p>LinkedIn</p>
          </div>

          <div className="text-right text-xs text-white/70 md:self-end">Privacy Policy</div>
        </div>

        <div className="mx-auto mt-8 w-full max-w-6xl border-t border-white/20 pt-4 text-xs text-white/70">
          All Rights Reserved © 2025 Second Brain
        </div>
      </footer>

      <motion.button
        type="button"
        onClick={() => navigate("/onboarding")}
        whileHover={{ scale: 1.04 }}
        whileTap={{ scale: 0.96 }}
        className="fixed bottom-6 right-6 flex items-center gap-2 rounded-full bg-[#7b45e1] px-4 py-3 text-xs font-semibold text-white shadow-xl"
      >
        Let&apos;s talk <ArrowUpRight className="h-4 w-4" />
      </motion.button>
    </div>
  );
};

export default Splash;
