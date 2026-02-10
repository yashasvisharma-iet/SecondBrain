import { useMemo, useState } from 'react'

import { GraphView } from '@/components/GraphView'
import { type GraphData } from '@/components/GraphTypes'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'

type Folder = {
  id: string
  name: string
}

type Note = {
  id: string
  folderId: string
  title: string
  content: string
  createdAt: string
}

const initialFolders: Folder[] = [
  { id: 'f1', name: 'General' },
  { id: 'f2', name: 'Ideas' },
]

const initialNotes: Note[] = [
  {
    id: 'n1',
    folderId: 'f1',
    title: 'MVP Architecture',
    content: 'Graph for navigation, Postgres for raw content, Neo4j for relationships only.',
    createdAt: 'Today',
  },
  {
    id: 'n2',
    folderId: 'f2',
    title: 'Welcome',
    content: 'This page should feel like a simple note-taking app with graph + editor.',
    createdAt: 'Today',
  },
]

export function Feed() {
  const [folders, setFolders] = useState<Folder[]>(initialFolders)
  const [notes, setNotes] = useState<Note[]>(initialNotes)
  const [activeFolderId, setActiveFolderId] = useState<string>(initialFolders[0].id)
  const [selectedNoteId, setSelectedNoteId] = useState<string>(initialNotes[0].id)

  const selectedNote = notes.find((note) => note.id === selectedNoteId) ?? null
  const visibleNotes = notes.filter((note) => note.folderId === activeFolderId)

  const graphData = useMemo<GraphData>(() => {
    const noteNodes = notes.map((note) => ({
      id: note.id,
      label: note.title || 'Untitled note',
      type: 'note' as const,
    }))

    const chunkNodes = notes.map((note) => ({
      id: `c-${note.id}`,
      label: `Chunk: ${note.title || 'Untitled'}`,
      type: 'chunk' as const,
    }))

    const noteToChunkEdges = notes.map((note) => ({ source: note.id, target: `c-${note.id}` }))

    const semanticEdges = notes.slice(1).map((note, index) => ({
      source: `c-${notes[index].id}`,
      target: `c-${note.id}`,
      score: Number((0.81 + index * 0.03).toFixed(2)),
    }))

    return {
      nodes: [...noteNodes, ...chunkNodes],
      edges: [...noteToChunkEdges, ...semanticEdges],
    }
  }, [notes])

  const handleAddFolder = () => {
    const newFolder: Folder = {
      id: `f${Date.now()}`,
      name: `New Folder ${folders.length + 1}`,
    }

    setFolders((current) => [...current, newFolder])
    setActiveFolderId(newFolder.id)
  }

  const handleAddNote = () => {
    const folderId = activeFolderId || folders[0]?.id
    if (!folderId) {
      return
    }

    const newNote: Note = {
      id: `n${Date.now()}`,
      folderId,
      title: 'Untitled note',
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

  const handleGraphSelect = (nodeId: string) => {
    const parsedId = nodeId.startsWith('c-') ? nodeId.slice(2) : nodeId
    const note = notes.find((item) => item.id === parsedId)

    if (!note) return

    setSelectedNoteId(note.id)
    setActiveFolderId(note.folderId)
  }

  return (
    <div className="flex h-screen w-full bg-background">
      <div className="min-w-0 flex-1 border-r bg-muted/20">
        <GraphView data={graphData} onNodeSelect={handleGraphSelect} />
      </div>

      <div className="flex w-[520px] flex-col bg-background">
        <div className="flex items-center justify-between border-b p-4">
          <h2 className="text-lg font-semibold">Notes</h2>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={handleAddFolder}>
              Add folder
            </Button>
            <Button size="sm" onClick={handleAddNote}>
              Add note
            </Button>
          </div>
        </div>

        <div className="border-b px-4 py-3">
          <div className="mb-3 flex flex-wrap gap-2">
            {folders.map((folder) => (
              <button
                key={folder.id}
                type="button"
                onClick={() => setActiveFolderId(folder.id)}
                className={`rounded-full border px-3 py-1 text-sm transition-colors ${
                  activeFolderId === folder.id ? 'border-primary bg-primary/10 text-primary' : 'hover:bg-muted'
                }`}
              >
                {folder.name}
              </button>
            ))}
          </div>

          <div className="space-y-2">
            {visibleNotes.length > 0 ? (
              visibleNotes.map((note) => (
                <button
                  key={note.id}
                  type="button"
                  onClick={() => setSelectedNoteId(note.id)}
                  className={`w-full rounded-md border p-2 text-left ${
                    selectedNoteId === note.id ? 'border-primary bg-primary/5' : 'hover:bg-muted'
                  }`}
                >
                  <p className="font-medium">{note.title}</p>
                  <p className="line-clamp-1 text-sm text-muted-foreground">{note.content || 'Empty note'}</p>
                </button>
              ))
            ) : (
              <p className="text-sm text-muted-foreground">No notes in this folder yet.</p>
            )}
          </div>
        </div>

        <div className="flex-1 space-y-3 p-4">
          <Input
            placeholder="Title"
            value={selectedNote?.title ?? ''}
            onChange={(event) => updateSelectedNote({ title: event.target.value })}
          />
          <Textarea
            placeholder="Start writing..."
            value={selectedNote?.content ?? ''}
            onChange={(event) => updateSelectedNote({ content: event.target.value })}
            className="h-[calc(100vh-280px)] min-h-[260px] resize-none"
          />
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <Badge variant="secondary">{selectedNote ? `Created: ${selectedNote.createdAt}` : 'No note selected'}</Badge>
          </div>
        </div>
      </div>
    </div>
  )
}
