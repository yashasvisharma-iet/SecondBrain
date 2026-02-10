// GraphPage.tsx
import { useState } from 'react'
import { GraphView } from '../components/GraphView'
import { RightPanel } from '../components/RightPanel'
import { GraphData } from '../components/GraphTypes'

const mockData: GraphData = {
  nodes: [
    { id: 'n1', label: 'Note: AI Ideas', type: 'note' },
    { id: 'n2', label: 'Note: Graphs', type: 'note' },
    { id: 'c1', label: 'Chunk: embeddings', type: 'chunk' },
    { id: 'c2', label: 'Chunk: similarity', type: 'chunk' },
  ],
  edges: [
    { source: 'n1', target: 'c1' },
    { source: 'n2', target: 'c2' },
    { source: 'c1', target: 'c2', score: 0.87 },
  ],
}

export function Feed() {
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)

  return (
    <div className="h-screen w-full flex bg-gray-50">
      {/* Left: Graph */}
      <div className="flex-1 border-r">
        <GraphView
          data={mockData}
          onNodeSelect={(id) => setSelectedNodeId(id)}
        />
      </div>

      {/* Right: Note / Chat */}
      <div className="w-[420px]">
        <RightPanel selectedNodeId={selectedNodeId} />
      </div>
    </div>
  )
}
