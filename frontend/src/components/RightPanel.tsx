// RightPanel.tsx
import { useState } from 'react'
import { NoteEditor } from './NoteEditor'
import { ChatPanel } from './ChatPanel'


export function RightPanel({ selectedNodeId }: { selectedNodeId: string | null }) {
  const [tab, setTab] = useState<'note' | 'chat'>('note')

  return (
    <div className="h-full flex flex-col bg-white">
      {/* Tabs */}
      <div className="flex border-b">
        <TabButton active={tab === 'note'} onClick={() => setTab('note')}>
          Note
        </TabButton>
        <TabButton active={tab === 'chat'} onClick={() => setTab('chat')}>
          AI Chat
        </TabButton>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto">
        {tab === 'note' && <NoteEditor selectedNodeId={selectedNodeId} />}
        {tab === 'chat' && <ChatPanel selectedNodeId={selectedNodeId} />}
      </div>
    </div>
  )
}

function TabButton({ active, children, onClick }) {
  return (
    <button
      onClick={onClick}
      className={`flex-1 py-2 text-sm font-medium ${
        active ? 'border-b-2 border-black' : 'text-gray-400'
      }`}
    >
      {children}
    </button>
  )
}
