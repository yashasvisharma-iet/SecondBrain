import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { GraphView } from '@/components/GraphView'
import { type GraphData } from '@/components/GraphTypes'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { apiUrl } from '@/lib/config'

type Note = {
  id: string
  title: string
  content: string
  createdAt: string
}

type GraphNodeMeta = {
  id: string
  label: string
  type: 'note' | 'chunk' | 'topic'
  genre?: string | null
}

const initialNotes: Note[] = [
  {
    id: 'n1',
    title: 'MVP Architecture',
    content: 'Graph for navigation, Postgres for raw content, Neo4j for relationships only.',
    createdAt: 'Today',
  },
  {
    id: 'n2',
    title: 'Welcome',
    content: 'This page should feel like a simple note-taking app with graph + editor.',
    createdAt: 'Today',
  },
]

const sidebarItems = [
  { id: 'brain-map', label: 'Brain map', icon: '🧠' },
  { id: 'notes', label: 'Notes', icon: '📝' },
  { id: 'knowledge', label: 'Knowledge', icon: '📚' },
  { id: 'connections', label: 'Connections', icon: '🔗' },
  { id: 'insights', label: 'Insights', icon: '📈' },
  { id: 'ai-agent', label: 'AI Agent', icon: '✨' },
]

type WorkspaceMode = (typeof sidebarItems)[number]['id']

type AppConnection = {
  id: string
  app: string
  description: string
  status: 'Connected' | 'Not connected'
}

type AgentCitation = {
  pageId: string
  chunkIndex: number
  snippet: string
  source: string
  syncedAt: string
}

type AgentResponse = {
  answer: string
  citations: AgentCitation[]
}

type NoteSummaryResponse = {
  summary: string
}

const connections: AppConnection[] = [
  { id: 'notion', app: 'Notion', description: 'Sync notes and pages', status: 'Connected' },
  { id: 'google-docs', app: 'Google Docs', description: 'Ingest docs and meeting notes', status: 'Not connected' },
  { id: 'drive', app: 'Google Drive', description: 'Import PDFs and files', status: 'Not connected' },
  { id: 'slack', app: 'Slack', description: 'Capture team knowledge snippets', status: 'Not connected' },
]

export function Feed() {
  const navigate = useNavigate()
  const [notes, setNotes] = useState<Note[]>(initialNotes)
  const [selectedNoteId, setSelectedNoteId] = useState<string>(initialNotes[0].id)
  const [searchTerm, setSearchTerm] = useState('')
  const [activeMode, setActiveMode] = useState<WorkspaceMode>('brain-map')
  const [isDetailOpen, setIsDetailOpen] = useState(false)
  const [agentQuery, setAgentQuery] = useState('')
  const [agentLoading, setAgentLoading] = useState(false)
  const [agentResponse, setAgentResponse] = useState<AgentResponse | null>(null)
  const [summaryLoading, setSummaryLoading] = useState(false)
  const [summaryText, setSummaryText] = useState('')
  const [showOnboardingReminder, setShowOnboardingReminder] = useState(() => sessionStorage.getItem('onboarding_complete') !== '1')

  const selectedNote = notes.find((note) => note.id === selectedNoteId) ?? null
  const visibleNotes = useMemo(() => {
    if (!searchTerm.trim()) return notes

    const query = searchTerm.toLowerCase()
    return notes.filter(
      (note) => note.title.toLowerCase().includes(query) || note.content.toLowerCase().includes(query),
    )
  }, [notes, searchTerm])

  const [graphData, setGraphData] = useState<GraphData>({ nodes: [], edges: [] })
  const graphNodeById = useMemo(() => {
    return new Map(graphData.nodes.map((node) => [node.id, node as GraphNodeMeta]))
  }, [graphData.nodes])

  const refreshGraph = async () => {
    try {
      const response = await fetch(apiUrl('/api/graph/feed'), { credentials: 'include' })
      if (!response.ok) return
      const data = (await response.json()) as GraphData
      setGraphData(data)
    } catch (error) {
      console.error('Failed to fetch graph data', error)
    }
  }

  const ingestNote = async (note: Pick<Note, 'id' | 'title' | 'content'>) => {
    const contentToIngest = note.content.trim() || note.title.trim() || 'Untitled note'
    const response = await fetch(apiUrl('/api/notion/ingestRaw'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ pageId: note.id, content: contentToIngest }),
    })

    if (!response.ok) {
      throw new Error(`Failed to ingest note ${note.id}`)
    }

    sessionStorage.setItem(`ingested_note_${note.id}`, contentToIngest)
  }

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void refreshGraph()
    }, 0)

    return () => window.clearTimeout(timer)
  }, [])

  useEffect(() => {
    const ingestNotes = async () => {
      try {
        const notesToIngest = notes.filter((note) => {
          const content = note.content.trim()
          if (!content) return false

          const handledKey = `ingested_note_${note.id}`
          const previousContent = sessionStorage.getItem(handledKey)
          return previousContent !== content
        })

        if (notesToIngest.length === 0) return

        const results = await Promise.all(
          notesToIngest.map(async (note) => {
            try {
              await ingestNote(note)
              return true
            } catch {
              return false
            }
          }),
        )

        if (results.some(Boolean)) {
          await refreshGraph()
        }
      } catch (error) {
        console.error('Failed to ingest notes', error)
      }
    }

    void ingestNotes()
  }, [notes])

  useEffect(() => {
    setSummaryText('')
  }, [selectedNoteId])

  const handleAddNote = () => {
    const noteNumber = notes.length + 1
    const newNote: Note = {
      id: `n${Date.now()}`,
      title: `New Note ${noteNumber}`,
      content: '',
      createdAt: 'Just now',
    }

    setNotes((current) => [newNote, ...current])
    setSelectedNoteId(newNote.id)
    setActiveMode('notes')

    void ingestNote(newNote)
      .then(() => refreshGraph())
      .catch((error) => {
        console.error('Failed to ingest newly added note', error)
      })
  }

  const updateSelectedNote = (updates: Partial<Pick<Note, 'title' | 'content'>>) => {
    if (!selectedNoteId) return

    setNotes((current) =>
      current.map((note) => {
        if (note.id !== selectedNoteId) return note
        return { ...note, ...updates }
      }),
    )
  }

  const resolveTitleFromContent = (content: string) => {
    const firstLine = content
      .split('\n')
      .map((line) => line.trim())
      .find((line) => line.length > 0)

    return firstLine ? firstLine.slice(0, 80) : 'Imported note'
  }

  const handleGraphSelect = async (nodeId: string) => {
    const parsedId = nodeId.startsWith('c-') ? nodeId.slice(2) : nodeId
    const graphNode = graphNodeById.get(parsedId)
    const existingNote = notes.find((item) => item.id === parsedId)

    if (existingNote) {
      setSelectedNoteId(existingNote.id)
      setActiveMode('brain-map')
      setIsDetailOpen(true)
      return
    }

    try {
      const sourceEndpoint = parsedId.startsWith('gdoc:')
        ? apiUrl(`/api/google-docs/doc/${encodeURIComponent(parsedId.slice(5))}`)
        : apiUrl(`/api/notion/page/${encodeURIComponent(parsedId)}`)

      const response = await fetch(sourceEndpoint, {
        credentials: 'include',
      })

      if (!response.ok) return

      const data = (await response.json()) as { pageId: string; content: string }

      const importedNote: Note = {
        id: data.pageId,
        title: graphNode?.label || resolveTitleFromContent(data.content ?? ''),
        content: data.content ?? '',
        createdAt: 'Imported',
      }

      setNotes((current) => [importedNote, ...current])
      setSelectedNoteId(importedNote.id)
      setActiveMode('brain-map')
      setIsDetailOpen(true)

    } catch (error) {
      console.error('Failed to open note from graph', error)
    }
  }

  const handleAgentAsk = async () => {
    if (!agentQuery.trim()) return

    setAgentLoading(true)
    try {
      const response = await fetch(apiUrl('/api/graph/ask'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ query: agentQuery.trim(), pageId: selectedNote?.id ?? null }),
      })

      if (!response.ok) {
        throw new Error('Agent request failed')
      }

      const data = (await response.json()) as AgentResponse
      setAgentResponse(data)
    } catch (error) {
      console.error('Failed to query AI agent', error)
      setAgentResponse({ answer: 'Could not reach the agent endpoint. Make sure the backend is running.', citations: [] })
    } finally {
      setAgentLoading(false)
    }
  }

  const openNoteFromAgent = (noteId: string) => {
    const existingNote = notes.find((note) => note.id === noteId)
    if (existingNote) {
      setSelectedNoteId(existingNote.id)
      setIsDetailOpen(true)
      setActiveMode('ai-agent')
      return
    }

    void handleGraphSelect(noteId)
  }

  const handleSummarizeNote = async () => {
    if (!selectedNote?.id) return

    setSummaryLoading(true)
    try {
      const response = await fetch(apiUrl('/api/graph/summarize'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ pageId: selectedNote.id }),
      })

      if (!response.ok) {
        throw new Error('Summary request failed')
      }

      const data = (await response.json()) as NoteSummaryResponse
      setSummaryText(data.summary)
    } catch (error) {
      console.error('Failed to summarize note', error)
      setSummaryText('Could not summarize this note right now. Please check if backend + AIML services are running.')
    } finally {
      setSummaryLoading(false)
    }
  }

  return (
    <div className="relative flex h-screen w-full flex-col overflow-hidden bg-[#d8d2e3]">
      {showOnboardingReminder ? (
        <div className="border-b border-[#d9ccff] bg-[#f5f0ff]">
          <div className="mx-auto flex w-full max-w-7xl flex-col gap-3 px-6 py-4 text-sm md:flex-row md:items-center md:justify-between">
            <div>
              <p className="font-semibold text-[#4b1e9b]">Finish setting up your workspace later</p>
              <p className="text-[#5c5570]">Skip onboarding for now and reconnect your note sources whenever you're ready.</p>
            </div>
            <div className="flex items-center gap-2">
              <Button type="button" onClick={() => navigate('/onboarding')}>
                Resume onboarding
              </Button>
              <Button
                type="button"
                variant="ghost"
                onClick={() => {
                  sessionStorage.setItem('onboarding_deferred', '1')
                  setShowOnboardingReminder(false)
                }}
              >
                Dismiss
              </Button>
            </div>
          </div>
        </div>
      ) : null}
      <div className="flex min-h-0 flex-1">
      <aside className="flex w-[320px] flex-col bg-[#2b0056] p-4 text-white">
        <Button
          type="button"
          onClick={handleAddNote}
          className="mb-6 h-14 w-full justify-start rounded-2xl bg-gradient-to-r from-[#9e70ff] to-[#7a58f2] px-5 text-3xl font-semibold hover:opacity-95"
        >
          + New Note
        </Button>

        <nav className="space-y-4 text-3xl">
          {sidebarItems.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => setActiveMode(item.id)}
              className="flex items-center gap-2 text-left text-white/95 transition-opacity hover:opacity-80"
            >
              <span>{item.icon}</span>
              <span className={activeMode === item.id ? 'font-semibold text-white' : ''}>{item.label}</span>
            </button>
          ))}
        </nav>
      </aside>

      <main className="flex min-w-0 flex-1 flex-col p-5">
        <div className="mb-4 flex items-center justify-between gap-4 rounded-xl bg-[#cdc6db] p-4">
          <div className="flex w-full max-w-xl items-center gap-3">
            <Input
              placeholder="Search notes, chunks, ideas..."
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
              className="h-12 border-0 bg-white text-base"
            />
            <Button type="button" variant="secondary" className="h-12 px-5">
              Search
            </Button>
          </div>
          <Badge variant="secondary" className="hidden rounded-lg bg-white px-3 py-2 text-sm font-medium text-[#3d3550] md:inline-flex">
            {sidebarItems.find((item) => item.id === activeMode)?.label}
          </Badge>
          <Button type="button" variant="secondary" className="h-12 rounded-xl bg-white px-5">
            🧠 You
          </Button>
        </div>

        <p className="mb-3 text-sm font-medium text-[#4f4565]">
          {activeMode === 'brain-map' && 'Explore your graph and open any node to see complete note context.'}
          {activeMode === 'notes' && 'Write and edit notes directly in Second Brain.'}
          {activeMode === 'knowledge' && 'Review distilled concepts from your current note collection.'}
          {activeMode === 'connections' && 'Inspect how ideas are linked across notes and chunks.'}
          {activeMode === 'insights' && 'Get revision-oriented prompts to resume exactly where you left off.'}
          {activeMode === 'ai-agent' && 'OpenAI-powered memory assistant that reasons over matched chunks and your selected note context.'}
        </p>

        <div className="grid min-h-0 flex-1 grid-cols-1 gap-4 xl:grid-cols-[340px_minmax(0,1fr)_360px]">
          <section className="flex min-h-0 flex-col rounded-2xl border border-white/50 bg-white/80 p-3">
            <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-[#3d3550]">Local notes</h2>
            <div className="min-h-0 flex-1 space-y-2 overflow-y-auto pr-1">
              {visibleNotes.length > 0 ? (
                visibleNotes.map((note) => (
                  <button
                    key={note.id}
                    type="button"
                    onClick={() => {
                      setSelectedNoteId(note.id)
                      setIsDetailOpen(true)
                    }}
                    className={`w-full rounded-lg border p-3 text-left ${
                      selectedNoteId === note.id ? 'border-[#7a58f2] bg-[#f1ecff]' : 'border-transparent bg-white hover:bg-[#f6f3ff]'
                    }`}
                  >
                    <div className="mb-1 flex items-center justify-between gap-2">
                      <p className="font-medium text-[#2f2147]">{note.title}</p>
                      {graphNodeById.get(note.id)?.genre && (
                        <Badge variant="secondary" className="rounded-md bg-[#ece7ff] text-[10px] font-semibold text-[#4b3f8d]">
                          {graphNodeById.get(note.id)?.genre}
                        </Badge>
                      )}
                    </div>
                    <p className="line-clamp-2 text-sm text-[#675f78]">{note.content || 'Empty note'}</p>
                  </button>
                ))
              ) : (
                <p className="text-sm text-[#675f78]">No notes found.</p>
              )}
            </div>

            <div className="mt-3 border-t pt-3">
              <Input
                placeholder="Title"
                value={selectedNote?.title ?? ''}
                onChange={(event) => updateSelectedNote({ title: event.target.value })}
                className="mb-2"
              />
              <Textarea
                placeholder="Start writing..."
                value={selectedNote?.content ?? ''}
                onChange={(event) => updateSelectedNote({ content: event.target.value })}
                className="h-24 resize-none"
              />
              <div className="mt-2 text-xs text-[#675f78]">
                <Badge variant="secondary">{selectedNote ? `Created: ${selectedNote.createdAt}` : 'No note selected'}</Badge>
              </div>
            </div>
          </section>

          {activeMode === 'brain-map' && (
            <section className="min-h-0 overflow-hidden rounded-2xl bg-[#d2cddb] p-3">
              <GraphView data={graphData} onNodeSelect={handleGraphSelect} />
            </section>
          )}

          {activeMode === 'notes' && (
            <section className="flex min-h-0 flex-col rounded-2xl border border-white/50 bg-white/80 p-4">
              <h2 className="mb-3 text-lg font-semibold text-[#2f2147]">Focused note editor</h2>
              <Input
                placeholder="Untitled"
                value={selectedNote?.title ?? ''}
                onChange={(event) => updateSelectedNote({ title: event.target.value })}
                className="mb-3"
              />
              <Textarea
                placeholder="Start writing..."
                value={selectedNote?.content ?? ''}
                onChange={(event) => updateSelectedNote({ content: event.target.value })}
                className="min-h-0 flex-1 resize-none"
              />
            </section>
          )}

          {activeMode === 'knowledge' && (
            <section className="min-h-0 rounded-2xl border border-white/50 bg-white/80 p-5 text-[#2f2147]">
              <h2 className="mb-2 text-lg font-semibold">Knowledge snapshot</h2>
              <p className="mb-4 text-sm text-[#675f78]">Quick concept recap generated from your latest selected note.</p>
              <p className="rounded-xl bg-[#f6f3ff] p-4 text-sm leading-6">
                {selectedNote?.content
                  ? `${selectedNote.content.slice(0, 350)}${selectedNote.content.length > 350 ? '...' : ''}`
                  : 'Select a note to see extracted key ideas and condensed concepts here.'}
              </p>
            </section>
          )}

          {activeMode === 'connections' && (
            <section className="min-h-0 rounded-2xl border border-white/50 bg-white/80 p-5 text-[#2f2147]">
              <h2 className="mb-2 text-lg font-semibold">App connections</h2>
              <p className="mb-4 text-sm text-[#675f78]">Manage source apps connected to your knowledge graph.</p>
              <div className="space-y-3">
                {connections.map((connection) => (
                  <div key={connection.id} className="flex items-center justify-between rounded-xl border border-[#e6e1f2] bg-[#faf8ff] px-4 py-3">
                    <div>
                      <p className="font-medium">{connection.app}</p>
                      <p className="text-xs text-[#675f78]">{connection.description}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge variant={connection.status === 'Connected' ? 'default' : 'secondary'}>{connection.status}</Badge>
                      <Button type="button" size="sm" variant="secondary">
                        {connection.status === 'Connected' ? 'Reconnect' : 'Connect'}
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
              <p className="mt-4 rounded-xl bg-[#f6f3ff] p-4 text-sm">Connected apps can ingest fresh content into your graph automatically.</p>
            </section>
          )}

          {activeMode === 'insights' && (
            <section className="min-h-0 rounded-2xl border border-white/50 bg-white/80 p-5 text-[#2f2147]">
              <h2 className="mb-2 text-lg font-semibold">Revision insights</h2>
              <div className="space-y-3 text-sm text-[#4d4560]">
                <p className="rounded-xl bg-[#f6f3ff] p-3">Continue where you left off: <span className="font-semibold">{selectedNote?.title ?? 'No note selected'}</span></p>
                <p className="rounded-xl bg-[#f6f3ff] p-3">Next action: Ask AI to quiz you on this note and suggest linked reading chunks.</p>
                <p className="rounded-xl bg-[#f6f3ff] p-3">Memory hint: revisit notes with fewer than 2 graph links this week.</p>
              </div>
            </section>
          )}

          {activeMode === 'ai-agent' && (
            <section className="flex min-h-0 flex-col rounded-2xl border border-white/50 bg-white/90 p-5">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="text-lg font-semibold text-[#2f2147]">AI Agent (full chat)</h2>
                  <p className="mt-1 text-sm text-[#675f78]">
                    Hi 👋 Yes, you wrote something about this. I&apos;ll use OpenAI reasoning on top of chunk matching so you get context-aware answers.
                  </p>
                </div>
              </div>
              <div className="mt-4 flex gap-2">
                <Input
                  value={agentQuery}
                  onChange={(event) => setAgentQuery(event.target.value)}
                  placeholder="e.g. what did I write about architecture?"
                />
                <Button type="button" onClick={handleAgentAsk} disabled={agentLoading || !agentQuery.trim()}>
                  {agentLoading ? 'Thinking...' : 'Ask AI'}
                </Button>
              </div>
              <p className="mt-2 text-xs text-[#675f78]">
                {selectedNote ? `Current note context: ${selectedNote.title}` : 'Tip: select a note for stronger contextual answers.'}
              </p>

              {agentResponse && (
                <div className="mt-4 space-y-3 overflow-y-auto rounded-xl bg-[#f8f6ff] p-4 text-sm text-[#2f2147]">
                  <p className="whitespace-pre-wrap">{agentResponse.answer}</p>
                  {agentResponse.citations.length > 0 && (
                    <ul className="space-y-2">
                      {agentResponse.citations.map((citation, index) => (
                        <li key={`${citation.pageId}-${citation.chunkIndex}-${index}`} className="rounded-lg bg-white p-3">
                          <div className="mb-2 flex items-center justify-between gap-2">
                            <p className="text-xs font-medium text-[#5d5470]">
                              {citation.source} / {citation.pageId} · {citation.syncedAt} · chunk {citation.chunkIndex}
                            </p>
                            <Button type="button" size="sm" variant="secondary" onClick={() => openNoteFromAgent(citation.pageId)}>
                              Open note link
                            </Button>
                          </div>
                          <p className="whitespace-pre-wrap">{citation.snippet}</p>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
            </section>
          )}

          <section className="flex min-h-0 flex-col rounded-2xl border border-white/50 bg-white/80 p-4">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-[#3d3550]">Node detail</h2>
              <Button type="button" variant="ghost" size="sm" onClick={() => setIsDetailOpen((open) => !open)}>
                {isDetailOpen ? 'Hide' : 'Show'}
              </Button>
            </div>

            {isDetailOpen && selectedNote ? (
              <>
                <p className="text-lg font-semibold text-[#2f2147]">{selectedNote.title || 'Untitled'}</p>
                {graphNodeById.get(selectedNote.id)?.genre && (
                  <Badge variant="secondary" className="mb-2 mt-1 w-fit rounded-md bg-[#ece7ff] text-xs font-semibold text-[#4b3f8d]">
                    Genre: {graphNodeById.get(selectedNote.id)?.genre}
                  </Badge>
                )}
                <p className="mb-3 text-xs text-[#675f78]">Source: {selectedNote.createdAt === 'Imported' ? 'Imported node' : 'Local note'}</p>
                <div className="min-h-0 flex-1 overflow-y-auto rounded-xl border bg-[#f8f6ff] p-3 text-sm leading-6 text-[#2f2147]">
                  {selectedNote.content || 'This note is empty. Add details to enrich your graph memory.'}
                </div>
                <div className="mt-3 grid grid-cols-1 gap-2">
                  <Button type="button" variant="secondary" className="justify-start" onClick={() => setActiveMode('notes')}>
                    Continue where I left off
                  </Button>
                  <Button type="button" variant="secondary" className="justify-start" onClick={handleSummarizeNote} disabled={summaryLoading}>
                    ✨ {summaryLoading ? 'Summarizing...' : 'Summarize with AI'}
                  </Button>
                  <Button type="button" variant="secondary" className="justify-start" onClick={handleAddNote}>
                    Add linked note
                  </Button>
                </div>
                {summaryText && (
                  <div className="mt-3 rounded-xl border border-[#e6e1f2] bg-[#faf8ff] p-3 text-sm text-[#2f2147]">
                    <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-[#675f78]">AI Summary</p>
                    <p className="whitespace-pre-wrap">{summaryText}</p>
                  </div>
                )}
              </>
            ) : (
              <p className="text-sm text-[#675f78]">Select a note or graph node to view full content and suggested next actions.</p>
            )}
          </section>
        </div>
      </main>
      </div>
    </div>
  )
}
