import { useEffect, useMemo, useState } from 'react'

import { GraphView } from '@/components/GraphView'
import { type GraphData } from '@/components/GraphTypes'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'

type Note = {
  id: string
  title: string
  content: string
  createdAt: string
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
]

export function Feed() {
  const [notes, setNotes] = useState<Note[]>(initialNotes)
  const [selectedNoteId, setSelectedNoteId] = useState<string>(initialNotes[0].id)
  const [searchTerm, setSearchTerm] = useState('')

  const selectedNote = notes.find((note) => note.id === selectedNoteId) ?? null
  const visibleNotes = useMemo(() => {
    if (!searchTerm.trim()) return notes

    const query = searchTerm.toLowerCase()
    return notes.filter(
      (note) => note.title.toLowerCase().includes(query) || note.content.toLowerCase().includes(query),
    )
  }, [notes, searchTerm])

  const [graphData, setGraphData] = useState<GraphData>({ nodes: [], edges: [] })

  const refreshGraph = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/graph/feed', { credentials: 'include' })
      if (!response.ok) return
      const data = (await response.json()) as GraphData
      setGraphData(data)
    } catch (error) {
      console.error('Failed to fetch graph data', error)
    }
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
            const response = await fetch('http://localhost:8080/api/notion/ingestRaw', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              credentials: 'include',
              body: JSON.stringify({ pageId: note.id, content: note.content }),
            })

            if (!response.ok) return false

            sessionStorage.setItem(`ingested_note_${note.id}`, note.content.trim())
            return true
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
    const existingNote = notes.find((item) => item.id === parsedId)

    if (existingNote) {
      setSelectedNoteId(existingNote.id)
      return
    }

    try {
      const response = await fetch(`http://localhost:8080/api/notion/page/${parsedId}`, {
        credentials: 'include',
      })

      if (!response.ok) return

      const data = (await response.json()) as { pageId: string; content: string }

      const importedNote: Note = {
        id: data.pageId,

        title: resolveTitleFromContent(data.content ?? ''),
        content: data.content ?? '',
        createdAt: 'Imported',
      }

      setNotes((current) => [importedNote, ...current])
      setSelectedNoteId(importedNote.id)

    } catch (error) {
      console.error('Failed to open note from graph', error)
    }
  }

  return (
    <div className="relative flex h-screen w-full overflow-hidden bg-[#d8d2e3]">
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
              className="flex items-center gap-2 text-left text-white/95 transition-opacity hover:opacity-80"
            >
              <span>{item.icon}</span>
              <span>{item.label}</span>
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
          <Button type="button" variant="secondary" className="h-12 rounded-xl bg-white px-5">
            🧠 You
          </Button>
        </div>

        <div className="grid min-h-0 flex-1 grid-cols-1 gap-4 xl:grid-cols-[340px_minmax(0,1fr)]">
          <section className="flex min-h-0 flex-col rounded-2xl border border-white/50 bg-white/80 p-3">
            <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-[#3d3550]">Local notes</h2>
            <div className="min-h-0 flex-1 space-y-2 overflow-y-auto pr-1">
              {visibleNotes.length > 0 ? (
                visibleNotes.map((note) => (
                  <button
                    key={note.id}
                    type="button"
                    onClick={() => setSelectedNoteId(note.id)}
                    className={`w-full rounded-lg border p-3 text-left ${
                      selectedNoteId === note.id ? 'border-[#7a58f2] bg-[#f1ecff]' : 'border-transparent bg-white hover:bg-[#f6f3ff]'
                    }`}
                  >
                    <p className="font-medium text-[#2f2147]">{note.title}</p>
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

          <section className="min-h-0 overflow-hidden rounded-2xl bg-[#d2cddb] p-3">
            <GraphView data={graphData} onNodeSelect={handleGraphSelect} />
          </section>
        </div>
      </main>

      <Button
        type="button"
        className="absolute bottom-6 right-6 rounded-full bg-[#5f48d8] px-6 py-5 text-base shadow-lg hover:bg-[#523dc0]"
        title="AI agent placeholder"
      >
        AI Agent
      </Button>
    </div>
  )
}
