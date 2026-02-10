// graph.types.ts
import { SimulationNodeDatum } from 'd3-force'
export type GraphNode = SimulationNodeDatum & {
  id: string
  label: string
  type: 'note' | 'chunk'
}

export type GraphEdge = {
  source: string | GraphNode
  target: string | GraphNode
  score?: number
}

export type GraphData = {
  nodes: GraphNode[]
  edges: GraphEdge[]
}